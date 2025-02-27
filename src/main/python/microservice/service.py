from flask import Flask, request, jsonify
import sys
import os

# Add parent directory to Python path to make imports work
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from llm import llm_prompt

app = Flask(__name__)

# Test route
@app.route('/multiply', methods=['POST'])
def multiply():
    data = request.get_json()
    x = data.get('x', 0)
    y = data.get('y', 0)
    result = x * y
    return jsonify({"message": f"The result of {x} * {y} is {result}"})
    
# Route for LLM
@app.route('/llm', methods=['POST'])
def llm():
    data = request.get_json()
    text_input = data.get('prompt', '')

    print(f"PythonService: Using model {data.get('model', 'bloom')}")

    model_name = data.get('model', 'bloom')  # Default to the fastest model
    result = llm_prompt(text_input, model_name)

    return jsonify({"message": result})

if __name__ == '__main__':
    app.run(port=5000)