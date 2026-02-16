"""
Utility script to drop and recreate all database tables.
USE ONLY IN DEVELOPMENT - this will delete all data!
"""

from db.models import Base
from db.session import engine

def recreate_tables():
    """Drop all tables and recreate them from the current ORM models."""
    print("Dropping all tables...")
    Base.metadata.drop_all(bind=engine)
    print("Creating all tables from current schema...")
    Base.metadata.create_all(bind=engine)
    print("✅ Database schema recreated successfully!")
    
    # Show all tables
    tables = list(Base.metadata.tables.keys())
    print(f"\nTables created: {', '.join(tables)}")

if __name__ == "__main__":
    recreate_tables()
