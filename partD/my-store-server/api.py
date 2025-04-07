from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
import os

from model_db import OrderModel, OrderProductModel, ProductModel, SupplierModel
from order import Order
from supplier import Supplier

# יצירת חיבור למסד הנתונים
db_path = os.path.join(os.path.dirname(__file__), 'database.db')
engine = create_engine(f"sqlite:///{db_path}")

SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)

store = APIRouter()

# תלות שיוצרת סשן חדש לכל בקשה
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

#  ניהול ספקים
class SupplierProductInput(BaseModel):
    name: str
    min_quantity: int
    price: float
class CreateSupplierRequest(BaseModel):
    representative_name: str
    phone_number: str
    company_name: str
    products: list[SupplierProductInput]
@store.post("/supplier")
async def create_supplier(request: CreateSupplierRequest, db: Session = Depends(get_db)):
    if not request.products:
        raise HTTPException(status_code=400, detail="חובה להוסיף לפחות מוצר אחד לספק חדש")

    supplier_controller = Supplier(db)
    try:
        supplier_id = supplier_controller.create(
            request.representative_name,
            request.phone_number,
            request.company_name
        )

        for product in request.products:
            supplier_controller.add_product(
                supplier_id,
                product.name,
                product.min_quantity,
                product.price
            )

        return {"message": "Supplier created successfully", "supplier_id": supplier_id}
    except Exception as e:
        return {"error": str(e)}

@store.get("/supplier/{id_supplier}")
async def get_supplier(id_supplier: str, db: Session = Depends(get_db)):
    supplier = db.query(SupplierModel).filter(SupplierModel.id == id_supplier).first()
    if not supplier:
        raise HTTPException(status_code=404, detail="הספק לא נמצא")
    
    return {
        "id": supplier.id,
        "representative_name": supplier.representative_name,
        "phone_number": supplier.phone_number,
        "company_name": supplier.company_name
    }

@store.get("/suppliers")
async def get_all_suppliers(db: Session = Depends(get_db)):
    """ קבלת כל הספקים """
    supplier_controller = Supplier(db)
    try:
        return {"suppliers": supplier_controller.get_all_suppliers()}
    except Exception as e:
        return {"error": str(e)}

@store.get("/supplier/{id_supplier}/products")
async def get_all_products_by_supplier_id(id_supplier: str, db: Session = Depends(get_db)):
    """ קבלת כל המוצרים של ספק מסוים """
    supplier_controller = Supplier(db)
    try:
        return {"products": supplier_controller.get_all_products_by_supplier_id(id_supplier)}
    except Exception as e:
        return {"error": str(e)}
    
class AddProductRequest(BaseModel):
    name: str
    min_quantity: int
    price: float
@store.post("/supplier/{id_supplier}/add_product")
async def add_product(id_supplier: str, request: AddProductRequest, db: Session = Depends(get_db)):
    """ הוספת מוצר לספק """
    supplier_controller = Supplier(db)
    try:
        product = supplier_controller.add_product(id_supplier, request.name, request.min_quantity, request.price)
        return {"message": "Product added successfully", "product_id": product.id}
    except Exception as e:
        return {"error": str(e)}

@store.get("/supplier/{id_supplier}/orders")
async def get_order_by_supplier_id(id_supplier: str, db: Session = Depends(get_db)):
    """ קבלת כל ההזמנות מספק מסוים """
    supplier_controller = Supplier(db)
    try:
        return {"orders": supplier_controller.get_order_by_supplier_id(id_supplier)}
    except Exception as e:
        return {"error": str(e)}

# ניהול הזמנות
class OrderProductInput(BaseModel):
    product_id: str
    count: int
class CreateOrderRequest(BaseModel):
    products: list[OrderProductInput]
@store.post("/supplier/{id_supplier}/order")
async def create_order(id_supplier: str, request: CreateOrderRequest, db: Session = Depends(get_db)):
    """ יצירת הזמנה חדשה לספק """
    order_controller = Order(db)
    try:
        if not request.products or len(request.products) == 0:
            raise HTTPException(status_code=400, detail="ההזמנה חייבת לכלול לפחות מוצר אחד.")

        order = order_controller.createOrder(supplier_id=id_supplier, status='created')  # מחזיר אובייקט OrderModel

        for product in request.products:
            if product.count <= 0:
                raise HTTPException(status_code=400, detail=f"הכמות למוצר {product.product_id} אינה תקינה.")

            order_product = OrderProductModel(
                order_id=order.id,
                product_id=product.product_id,
                quantity=product.count
            )
            db.add(order_product)
        db.commit()
        return {"message": "Order created successfully", "order_id": order.id}
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))
   
@store.get("/orders/status")
async def get_status_orders(db: Session = Depends(get_db)):
    """ קבלת סטטוס הזמנות קיימות """
    order_controller = Order(db)
    try:
        orders = order_controller.get_status_orders()
        return [{"order_id": order_id, "status": status} for order_id, status in orders]
    except Exception as e:
        return {"error": str(e)}
    
class UpdateStatusRequest(BaseModel):
    status: str
@store.put("/order/{order_id}/update_status")
async def approve_order(order_id: str, db: Session = Depends(get_db)):
    """ ספק מאשר שההזמנה בטיפול """
    order_controller = Order(db)  
    try:
        order_controller.approve_order(order_id)
        return {"message": "Order status updated to 'In Progress'"}
    except Exception as e:
        return {"error": str(e)}

@store.put("/order/{order_id}/complete")
async def complete_order(order_id: str, db: Session = Depends(get_db)):
    """ בעל המכולת מעדכן שההזמנה הושלמה """
    order_controller = Order(db)  
    try:
        order_controller.complete_order(order_id)
        return {"message": "Order marked as completed"}
    except Exception as e:
        return {"error": str(e)}
    
@store.get("/orders_with_products")
async def get_orders_with_products(db: Session = Depends(get_db)):
    """ מחזיר את כל ההזמנות כולל רשימת המוצרים בכל אחת """
    try:
        orders = db.query(OrderModel).all()
        result = []
        for order in orders:
            products = (
                db.query(OrderProductModel)
                .filter_by(order_id=order.id)
                .all()
            )

            product_list = []
            for op in products:
                product = db.query(ProductModel).filter_by(id=op.product_id).first()
                if product:
                    product_list.append({
                        "product_id": product.id,
                        "name": product.name,
                        "price": product.price,
                        "quantity": op.quantity
                    })
            result.append({
                "order_id": order.id,
                "supplier_id": order.supplier_id,
                "status": order.status,
                "products": product_list
            })
        return {"orders": result}
    except Exception as e:
        return {"error": str(e)}

@store.get("/orders_with_products")
async def get_orders_with_products(db: Session = Depends(get_db)):
    order_controller = Order(db)
    try:
        orders = order_controller.get_all_orders_with_products()
        return {"orders": orders}
    except Exception as e:
        return {"error": str(e)}



