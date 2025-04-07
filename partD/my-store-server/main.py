from fastapi import FastAPI
from api import store
from fastapi.middleware.cors import CORSMiddleware


app = FastAPI()
app.include_router(store, prefix="/store")



app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

