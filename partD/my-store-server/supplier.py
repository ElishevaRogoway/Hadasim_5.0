from model_db import OrderModel, ProductModel, SupplierModel, session

class Supplier:
    
    def __init__(self, session):
        self.session = session

    #יצירת ספק
    def create(self, representative_name: str, phone_number: str, company_name: str) -> int:
        try:
            supplier = SupplierModel(representative_name=representative_name, phone_number=phone_number, company_name=company_name)
            self.session.add(supplier)
            self.session.commit()
            return supplier.id
        except Exception as e:
            self.session.rollback()
            raise ValueError(f"Error creating supplier: {str(e)}")
    
    #הוספת מוצר לספק
    def add_product(self, supplier_id: str, name: str, min_quantity: int, price: float):
        product = ProductModel(supplier_id=supplier_id, name=name, min_quantity=min_quantity, price=price)
        self.session.add(product)
        self.session.commit()
        self.session.refresh(product)
        return product 

    #שליפת כל המוצרים של ספק מסוים
    def get_all_products_by_supplier_id(self, supplier_id: str):
        return self.session.query(ProductModel).filter_by(supplier_id=supplier_id).all()

    #שליפת כל הספקים במערכת
    def get_all_suppliers(self):
        suppliers = self.session.query(SupplierModel).all()
        return [
            {
            "id": s.id,
            "name": s.representative_name,
            "phone": s.phone_number,
            "company_name": s.company_name
            }
            for s in suppliers
        ]

    #שליפת כל ההזמנות מספק מסוים
    def get_order_by_supplier_id(self, supplier_id: str):
        return self.session.query(OrderModel).filter_by(supplier_id=supplier_id).all()
    



