# Use a pipeline as a high-level helper
from transformers import pipeline

'''
Generate text 
'''
# Tested models
MODELS = {
    "bloom": "bigscience/bloom-1b1",
    "starcoder-3b": "bigcode/starcoder2-3b",
    "starcoder-15b": "bigcode/starcoder2-15b"
}

def llm_prompt(input, model_name="bloom"):
    # Default to bloom as it is the fastest model
    model = MODELS.get(model_name, MODELS["bloom"])

    print(f"PythonLLMFunction: Model set to {model_name}")

    # Load pipeline
    pipe = pipeline("text-generation", model=model)

    # Generate text
    result = pipe(input, max_length=50)
    answer = result[0]['generated_text']
    
    return answer
