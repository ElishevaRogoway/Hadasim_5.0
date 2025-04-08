from model_db import OrderModel, OrderProductModel, ProductModel, Session

class Order:
   
    def __init__(self, session):
        self.session = session

    #Create a new order with the given supplier ID and initial status
    def createOrder(self, supplier_id: str, status: str ):
        try:
            order = OrderModel(supplier_id=supplier_id, status=status)
            self.session.add(order)
            self.session.flush()
            return order
          
        except Exception as e:
            self.session.rollback()
            raise ValueError(f"Failed to create order: {str(e)}")

    #Add a product to an existing order
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

    # Get a list of all non-completed orders' status
    def get_status_orders(self):
        return (self.session.query(OrderModel.id, OrderModel.status)
        .filter(OrderModel.status != "completed").all()
    )

    # Update an order status to 'In progress' to indicate supplier approval.
    def approve_order(self, order_id: str):
        order = self.session.query(OrderModel).filter_by(id=order_id).first()
        if not order:
            raise ValueError("Order not found")
        order.status = "In progress"
        self.session.commit()

    # Mark an order as 'completed' to indicate the store owner received it.
    def complete_order(self, order_id: str):
        order = self.session.query(OrderModel).filter_by(id=order_id).first()
        if not order:
            raise ValueError("Order not found")
        order.status = "completed"
        self.session.commit()

    


