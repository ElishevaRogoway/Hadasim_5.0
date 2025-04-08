# --- FastAPI Router for Managing Suppliers, Products, and Orders ---

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, Session
import os

from model_db import OrderModel, OrderProductModel, ProductModel, SupplierModel
from order import Order
from supplier import Supplier

# Setup database connection
db_path = os.path.join(os.path.dirname(__file__), 'database.db')
engine = create_engine(f"sqlite:///{db_path}")
SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)

store = APIRouter()

# Dependency: creates a new DB session per request
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Supplier

# Request body for supplier creation
class SupplierProductInput(BaseModel):
    name: str
    min_quantity: int
    price: float

class CreateSupplierRequest(BaseModel):
    representative_name: str
    phone_number: str
    company_name: str
    products: list[SupplierProductInput]

# Create a new supplier with a list of products
@store.post("/supplier")
async def create_supplier(request: CreateSupplierRequest, db: Session = Depends(get_db)):
    if not request.products:
        raise HTTPException(status_code=400, detail="At least one product is required for a new supplier.")
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

# Get a supplier by ID
@store.get("/supplier/{id_supplier}")
async def get_supplier(id_supplier: str, db: Session = Depends(get_db)):
    supplier = db.query(SupplierModel).filter(SupplierModel.id == id_supplier).first()
    if not supplier:
        raise HTTPException(status_code=404, detail="Supplier not found")
    return {
        "id": supplier.id,
        "representative_name": supplier.representative_name,
        "phone_number": supplier.phone_number,
        "company_name": supplier.company_name
    }

# Get all suppliers
@store.get("/suppliers")
async def get_all_suppliers(db: Session = Depends(get_db)):
    """Returns a list of all suppliers in the system"""
    supplier_controller = Supplier(db)
    try:
        return {"suppliers": supplier_controller.get_all_suppliers()}
    except Exception as e:
        return {"error": str(e)}

# Get all products of a specific supplier
@store.get("/supplier/{id_supplier}/products")
async def get_all_products_by_supplier_id(id_supplier: str, db: Session = Depends(get_db)):
    """Returns all products for a given supplier ID"""
    supplier_controller = Supplier(db)
    try:
        return {"products": supplier_controller.get_all_products_by_supplier_id(id_supplier)}
    except Exception as e:
        return {"error": str(e)}

# Add a product to an existing supplier
class AddProductRequest(BaseModel):
    name: str
    min_quantity: int
    price: float

@store.post("/supplier/{id_supplier}/add_product")
async def add_product(id_supplier: str, request: AddProductRequest, db: Session = Depends(get_db)):
    """Adds a new product to an existing supplier"""
    supplier_controller = Supplier(db)
    try:
        product = supplier_controller.add_product(id_supplier, request.name, request.min_quantity, request.price)
        return {"message": "Product added successfully", "product_id": product.id}
    except Exception as e:
        return {"error": str(e)}

# Get all orders by supplier ID
@store.get("/supplier/{id_supplier}/orders")
async def get_order_by_supplier_id(id_supplier: str, db: Session = Depends(get_db)):
    """Returns all orders made from a specific supplier"""
    supplier_controller = Supplier(db)
    try:
        return {"orders": supplier_controller.get_order_by_supplier_id(id_supplier)}
    except Exception as e:
        return {"error": str(e)}

# Order Endpoints 

# Models for order creation
class OrderProductInput(BaseModel):
    product_id: str
    count: int

class CreateOrderRequest(BaseModel):
    products: list[OrderProductInput]

# Create a new order for a supplier
@store.post("/supplier/{id_supplier}/order")
async def create_order(id_supplier: str, request: CreateOrderRequest, db: Session = Depends(get_db)):
    """Creates a new order for a specific supplier with selected products"""
    order_controller = Order(db)
    try:
        if not request.products:
            raise HTTPException(status_code=400, detail="Order must include at least one product.")
        order = order_controller.createOrder(supplier_id=id_supplier, status='created')
        for product in request.products:
            if product.count <= 0:
                raise HTTPException(status_code=400, detail=f"Invalid quantity for product {product.product_id}.")
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

# Get all orders with status not completed
@store.get("/orders/status")
async def get_status_orders(db: Session = Depends(get_db)):
    """Returns orders that are not yet completed"""
    order_controller = Order(db)
    try:
        orders = order_controller.get_status_orders()
        return [{"order_id": order_id, "status": status} for order_id, status in orders]
    except Exception as e:
        return {"error": str(e)}

# Supplier approves an order (mark as in progress)
@store.put("/order/{order_id}/update_status")
async def approve_order(order_id: str, db: Session = Depends(get_db)):
    """Updates order status to 'In Progress' (by supplier)"""
    order_controller = Order(db)
    try:
        order_controller.approve_order(order_id)
        return {"message": "Order status updated to 'In Progress'"}
    except Exception as e:
        return {"error": str(e)}

# Mark an order as completed (by store owner)
@store.put("/order/{order_id}/complete")
async def complete_order(order_id: str, db: Session = Depends(get_db)):
    """Marks an order as 'Completed' (by store owner)"""
    order_controller = Order(db)
    try:
        order_controller.complete_order(order_id)
        return {"message": "Order marked as completed"}
    except Exception as e:
        return {"error": str(e)}

# Get all orders including full product details for each
@store.get("/orders_with_products")
async def get_orders_with_products(db: Session = Depends(get_db)):
    """Returns all orders with their corresponding product details"""
    try:
        orders = db.query(OrderModel).all()
        result = []
        for order in orders:
            products = db.query(OrderProductModel).filter_by(order_id=order.id).all()
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


