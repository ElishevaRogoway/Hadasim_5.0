import uuid
from sqlalchemy import create_engine, Column, String, Integer, Float, ForeignKey, Table
from sqlalchemy.orm import declarative_base, sessionmaker, relationship

# התחברות למסד נתונים SQLite
db_path = r"C:\Users\geisl\Documents\Hadasim Project\partD\database.db"
engine = create_engine(f"sqlite:///{db_path}", echo=True)

Session = sessionmaker(bind=engine)
session = Session()
Base = declarative_base()

#טבלת ספקים
class SupplierModel(Base):
    __tablename__ = 'suppliers'
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    representative_name = Column(String)
    phone_number = Column(String)
    company_name = Column(String)

#טבלת מוצרים
class ProductModel(Base):
    __tablename__ = 'products'
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    supplier_id = Column(String, ForeignKey('suppliers.id'))
    name = Column(String)
    min_quantity = Column(Integer)
    price = Column(Float)

#טבלת הזמנות
class OrderModel(Base):
    __tablename__ = 'orders'
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    supplier_id = Column(String, ForeignKey('suppliers.id'))
    status = Column(String)

#טבלת הקשרים בין הזמנות למוצרים
class OrderProductModel(Base):
    __tablename__ = 'order_products'
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    order_id = Column(String, ForeignKey('orders.id'))
    product_id = Column(String, ForeignKey('products.id'))
    quantity = Column(Integer)

# יצירת הטבלאות במסד
Base.metadata.create_all(engine)