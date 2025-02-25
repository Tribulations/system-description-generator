from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/multiply', methods=['POST'])
def multiply():
    data = request.get_json()
    x = data.get('x', 0)
    y = data.get('y', 0)
    result = x * y
    return jsonify({"message": f"The result of {x} * {y} is {result}"})

if __name__ == '__main__':
    app.run(port=5000)