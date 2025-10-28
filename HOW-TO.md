# Quick Start: System Description Generator (SDG)

**Launch:**
Run the app and wait for the UI to load.

**Workflow:**

1. **Browse** – Select a local project or enter a GitHub URL.
2. **Process** – Analyze code and build the knowledge graph.
3. **Generate description** – Generate a high-level textual description and multiple PlantUML diagrams.
4. **Viewing** – Access rendered descriptions in the tabs (textual selected by default).

**Output:**
The generated descriptions (textual and graphical), JSON context, and plantuml for each diagram
are automatically saved in the `sdg-out/<systemName>/` directory.

**Logging**: 
- The app logs to `logs/sdg.log`
- Modify `src/main/resources/logback.xml` to configure logging e.g., log level

**LLM Configuration:**
- Set API keys in `.env`. See [env.template](.env.template) for more information.
- If both a Gemini and Claude API key are set, *Gemini* will be used as default.
- Commented-out models provide better quality but are slower, more costly,
- and frequently fail due to rate limits (tokens per minute).
- To handle this, a 62-second delay is applied when using these models.
- The delay can be modified in method MainController::generateAndDisplayDiagrams.
- The older models frequently fail to generate some diagrams so using newer models is recommended when possible.
- Of the four models that have been tested, the Anthropic/Claude model claude-sonnet-4-5-20250929 appears to perform the best. 
