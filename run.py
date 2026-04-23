import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from src.api.routes import app

if __name__ == "__main__":
    app.run(debug=True, port=5000)