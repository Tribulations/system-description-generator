# Use a pipeline as a high-level helper
from transformers import pipeline
import torch

# Specify GPU device if available, otherwise use CPU
# Make sure you have the appropriate CUDA toolkit and GPU drivers installed for PyTorch to recognize and use your GPU.
device = 0 if torch.cuda.is_available() else -1  # 0 means first GPU, -1 means CPU

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
    pipe = pipeline("text-generation", model=model, device=device, torch_dtype=torch.float16, return_full_text=False)

    # Generate text
    result = pipe(input, max_length=400)
    answer = result[0]['generated_text']
    
    return answer
