from model_db import OrderModel, OrderProductModel, ProductModel, Session

class Order:
    #חיבור למסד נתונים
    def __init__(self, session):
        self.session = session

    #יוצר הזמנה חדשה
    def createOrder(self, supplier_id: str, status: str ):
        try:
            order = OrderModel(supplier_id=supplier_id, status=status)
            self.session.add(order)
            self.session.flush()
            return order
          
        except Exception as e:
            self.session.rollback()
            raise ValueError(f"Failed to create order: {str(e)}")


    def add_product_to_order(self, order_id: str, product_id: str, count: int) -> float:
        product = self.session.query(ProductModel).filter_by(id=product_id).first() #בודק אם המוצר קיים
        if not product:
            raise ValueError(f"Product with ID {product_id} not found")

        if count < product.min_quantity: #בודק אם הכמות נמוכה מהמינימום
            raise ValueError("Quantity is below minimum")
                  
        order_product = OrderProductModel(order_id=order_id, product_id=product_id, quantity=count)
        self.session.add(order_product)
        self.session.commit()

        return product.price * count

    # צפייה בסטטוס ההזמנות קיימות
    def get_status_orders(self):
        return (self.session.query(OrderModel.id, OrderModel.status)
        .filter(OrderModel.status != "completed").all()
    )

    #אישור ספק
    def approve_order(self, order_id: str):
        order = self.session.query(OrderModel).filter_by(id=order_id).first()
        if not order:
            raise ValueError("Order not found")
        order.status = "In progress"
        self.session.commit()

    #אישור בעל מכולת על קבלת הזמנה
    def complete_order(self, order_id: str):
        order = self.session.query(OrderModel).filter_by(id=order_id).first()
        if not order:
            raise ValueError("Order not found")
        order.status = "completed"
        self.session.commit()

    


