# Use a pipeline as a high-level helper
from transformers import pipeline

'''
Generate text 
'''
def llm_prompt(input):
    # Tested models
    model1 = "bigscience/bloom-1b1"
    model2 = "bigcode/starcoder2-3b"
    model3 = "bigcode/starcoder2-15b"

    # Load pipeline
    pipe = pipeline("text-generation", model=model1)

    # Generate text
    result = pipe(input, max_length=50)
    answer = result[0]['generated_text']
    
    return answer
