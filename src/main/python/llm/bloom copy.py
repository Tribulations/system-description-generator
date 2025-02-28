# Use a pipeline as a high-level helper
from transformers import pipeline
import torch

# Specify GPU device if available, otherwise use CPU
# Make sure you have the appropriate CUDA toolkit and GPU drivers installed for PyTorch to recognize and use your GPU.
device = 0 if torch.cuda.is_available() else -1  # 0 means first GPU, -1 means CPU

# For even better performance with large models like StarCoder2-15B, you can enable half-precision (fp16) to reduce memory usage and increase speed:
# pipe = pipeline(
#     "text-generation", 
#     model=model3, 
#     device=0,
#     torch_dtype=torch.float16  # Use half-precision
# )

# Tested models
model1 = "bigscience/bloom-1b1"
model2 = "bigcode/starcoder2-3b"
model3 = "bigcode/starcoder2-15b"

# Load pipeline
pipe = pipeline("text-generation", model=model1)
# Load pipeline and use gpu
# pipe = pipeline("text-generation", model=model2, device=device)
# Load pipeline, use gpu, and enabling half-precision
# pipe = pipeline("text-generation", model=model1, device=device, torch_dtype=torch.float16)

prompt1 = "Only provide a concise answer to the following question: What is a for loop in Java?"
prompt2 = "Answer yes or no to the following question: Is int a = 5; a correct variable assignment in Java?"
small_kg = """
"UserAuthenticationModule": {
  "calls": ["DatabaseConnector", "LoggingService", "TokenManager"]
}
"""

big_kg = """
{
  "KnowledgeGraph": {
    "relations": {
      "UserAuthenticationModule": {
        "calls": ["DatabaseConnector", "LoggingService", "TokenManager"],
        "implements": ["OAuth2.0Protocol", "TwoFactorAuthentication"],
        "sends_events_to": ["AuditSystem", "NotificationService"],
        "dependencies": ["ConfigurationManager", "EncryptionLibrary"]
      }
    }
  }
}
"""

prompt3 = f"""
Given the following knowledge graph in JSON format:

{big_kg}

Provide a short high-level description of the 'UserAuthenticationModule' component, explaining:
1. What other components it interacts with
2. What protocols it implements
3. What events it triggers
4. What dependencies it has
5. Its likely overall responsibility in the system architecture

Description:
"""

def llm_prompt(input):
  prompt = f"""
  Given the following knowledge graph in JSON format:

  {input}

  Provide a short high-level description of the 'UserAuthenticationModule' component, explaining:
  1. What other components it interacts with
  2. What protocols it implements
  3. What events it triggers
  4. What dependencies it has
  5. Its likely overall responsibility in the system architecture

  Description:
  """

  # Generate text
  result = pipe(input, max_length=50)
  answer = result[0]['generated_text']
  return answer

# answer = llm_prompt("What is France?")
# print(answer)

# # Load model directly
# from transformers import AutoTokenizer, AutoModelForCausalLM
#
# tokenizer = AutoTokenizer.from_pretrained("bigscience/bloom-1b1")
# model = AutoModelForCausalLM.from_pretrained("bigscience/bloom-1b1")

