from langchain.document_loaders import TextLoader
from langchain.indexes import VectorstoreIndexCreator
from langchain.chains import ConversationalRetrievalChain
from langchain.chat_models import ChatOpenAI
import sys
import os

os.environ["OPENAI_API_KEY"] = "klic"

def main():
    # Validate command-line arguments
    if len(sys.argv) != 2:
        print("Usage: python main.py <prompt>")
        sys.exit(1)

    # Get the prompt from command-line arguments
    prompt = sys.argv[1]

    file_path = "data.csv"
    data = [TextLoader(file_path, encoding='utf-8')]

    # Check if OPENAI_API_KEY is set before creating VectorstoreIndexCreator
    if not os.getenv("OPENAI_API_KEY"):
        print("Error: OPENAI_API_KEY is not set.")
        sys.exit(1)

    # Create VectorstoreIndexCreator and load the data
    try:
        index = VectorstoreIndexCreator().from_loaders(data)
    except Exception as e:
        print(f"Error creating index: {e}")
        sys.exit(1)

    # Create ConversationalRetrievalChain
    chain = ConversationalRetrievalChain.from_llm(
        llm=ChatOpenAI(model="gpt-3.5-turbo"),
        retriever=index.vectorstore.as_retriever(search_kwargs={"k": 1}),
    )

    chat_history = []
    query = prompt

    if query.lower() in ['quit', 'q', 'exit']:
        sys.exit()

    result = chain({"question": query, "chat_history": chat_history})
    print(result['answer'])

    # Update chat history for the next iteration
    chat_history.append({"role": "system", "content": "You: " + query})
    chat_history.append({"role": "assistant", "content": result['answer']})

if __name__ == "__main__":
    main()
