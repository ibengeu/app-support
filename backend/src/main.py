from agent import agent

if __name__ == "__main__":
    print("Chat with the support agent. Type 'quit' to end.")

    while True:
        question = input("You: ").strip()

        if question.lower() == "quit":
            print("Goodbye!")
            break

        if not question:
            continue

        result = agent.invoke(
            {"messages": [{"role": "user", "content": question}]},
            config={"configurable": {"thread_id": "cli"}},
        )
        print(f"Agent: {result['messages'][-1].content}")
