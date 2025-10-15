package com.sdg.diagrams;

public final class PlantUMLTestData {
    private PlantUMLTestData() {
        throw new UnsupportedOperationException("Test data class cannot be instantiated");
    }

    public static final String VALID_THREE_DIAGRAMS = """
            @startuml
            some syntax
            @enduml
            
            @startuml
            ...
            ...
            @enduml
            
            @startuml
            ---
            ---
            @enduml""";

    public static final String ONE_DIAGRAM_MISSING_START = """
            some syntax
            @enduml
            
            @startuml
            ...
            @enduml
            
            @startuml
            ---
            @enduml""";

    public static final String ONE_DIAGRAM_MISSING_END = """
            @startuml
            some syntax
            
            @startuml
            ...
            @enduml
            
            @startuml
            ---
            @enduml""";

    public static final String CLAUDE_GENERATED_PLANT_UML = """
            @startuml Architecture
            !theme plain
            skinparam componentStyle rectangle
            
            package "View Layer" {
              [MainView]
              [MazeView]
            }
            
            package "Controller Layer" {
              [MainController]
            }
            
            package "Model Layer" {
              [MazeGrid]
              [MazeGenerator]
              package "Algorithms" {
                [AStar]
                [DijkstraPriorityQueue]
                [AStarOOP]
                [DijkstraArrayList]
              }
            }
            
            MainView --> MainController
            MazeView --> MainController
            MainController --> MazeGrid
            MainController --> MazeGenerator
            MainController --> Algorithms
            MazeGenerator --> MazeGrid
            @enduml
            
            @startuml Components
            !theme plain
            skinparam componentStyle rectangle
            
            interface MainViewListener
            interface AlgorithmListener
            
            component MainView
            component MazeView
            component MainController
            component MazeGenerator
            component MazeGrid
            
            package "Algorithms" {
              component AStar
              component DijkstraPriorityQueue
              component AStarOOP
              component DijkstraArrayList
            }
            
            MainView -( MainViewListener
            MainController --|> MainViewListener
            MainController --|> AlgorithmListener
            Algorithms -( AlgorithmListener
            
            MainView --> MazeView
            MainController --> MazeGenerator
            MainController --> MazeGrid
            MainController --> Algorithms
            MazeGenerator --> MazeGrid
            @enduml
            
            @startuml DataFlow
            !theme plain
            start
            
            :User interacts with MainView;
            note right: Step 1\\nUser inputs maze parameters or loads image
            
            :MainController receives event;
            note right: Step 2\\nMainController processes user input
            
            fork
              :MazeGenerator creates maze;
              note right: Step 3a\\nGenerate maze structure
            fork again
              :MazeGrid initializes;
              note right: Step 3b\\nPrepare data structure
            end fork
            
            :MainController selects algorithm;
            note right: Step 4\\nChoose solving algorithm
            
            :Algorithm processes maze;
            note right: Step 5\\nExecute pathfinding
            
            :MazeView updates display;
            note right: Step 6\\nRender solution path
            
            stop
            @enduml
            """;

    public static final String INVALID_DIAGRAM_SYNTAX = """
            @startuml
            (*) --> "Receive User Input";
            note right: Step 1\nUser input received by Controller
        
            "Receive User Input" --> "Pass Input to Chatbot";
            note right: Step 2\nController passes input to Chatbot
        
            "Pass Input to Chatbot" --> "Determine User Intent";
            note right: Step 3\nChatbot uses IntentDetector
        
            "Determine User Intent" --> "Manage Conversation Context";
            note right: Step 4\nConversationHelper manages context
        
            "Manage Conversation Context" --> fork
            fork --> "Retrieve Cryptocurrency Prices"
            note right: Step 5\nIf intent involves crypto prices, retrieve data from CoinbasePriceFeed
            fork --> "Generate Response"
            note right: Step 6\nChatbot generates response
        
            end fork --> "Send Response to Controller";
            note right: Step 7\nChatbot sends response to Controller
        
            "Send Response to Controller" --> (*)
        
            @enduml
        
            """;

    public static final String INVALID_PLANT_UML_SYNTAX_SOURCE =
            """
            @startuml High_Level_Architecture
            !theme plain
            title Maze Solving Application - High-Level Architecture (MVC Pattern)
            
            package "View Layer" {
                [MainView]
                [MazeView
                [MazeSelectorView]
            }
            
            package "Controller Layer" {
                [MainController]
            }
            
            package "Model Layer" {
                package "Algorithm Components" {
                    [BaseAlgorithm]
                    [DijkstraPriorityQueue]
                    [DijkstraArrayList]
                    [AStar]
                    [AStarOOP]
                }
                
                package "Maze Components" {
                    [MazeGenerator]
                    [MazeGrid]
                }
            }
            
            package "Core Technologies" {
                [Java Swing]
                [OpenCV]
                [Java AWT]
                [Java Collections]
                [Java IO]
            }
            
            ' MVC relationships
            [MainView] --> [MainController]
            [MazeView] --> [MainController]
            [MazeSelectorView] --> [MainController]
            [MainController] --> [BaseAlgorithm]
            [MainController] --> [MazeGenerator]
            [MainController] --> [MazeGrid]
            
            ' Algorithm implementations
            [DijkstraPriorityQueue] ..|> [BaseAlgorithm]
            [DijkstraArrayList] ..|> [BaseAlgorithm]
            [AStar] ..|> [BaseAlgorithm]
            [AStarOOP] ..|> [BaseAlgorithm]
            
            ' Maze components relationship
            [MazeGenerator] --> [MazeGrid]
            
            ' Technology dependencies
            [MainView] ..> [Java Swing]
            [MazeView] ..> [Java AWT]
            [MazeSelectorView] ..> [Java Swing]
            [MazeGenerator] ..> [OpenCV]
            [BaseAlgorithm] ..> [Java Collections]
            [MazeGenerator] ..> [Java IO]
            @enduml
            """;
    public static final String VALID_PLANT_UML_SYNTAX =
            """
            @startuml High_Level_Architecture
            !theme plain
            title Maze Solving Application - High-Level Architecture (MVC Pattern)
            
            package "View Layer" {
                [MainView]
                [MazeView]
                [MazeSelectorView]
            }
            
            package "Controller Layer" {
                [MainController]
            }
            
            package "Model Layer" {
                package "Algorithm Components" {
                    [BaseAlgorithm]
                    [DijkstraPriorityQueue]
                    [DijkstraArrayList]
                    [AStar]
                    [AStarOOP]
                }
                
                package "Maze Components" {
                    [MazeGenerator]
                    [MazeGrid]
                }
            }
            
            package "Core Technologies" {
                [Java Swing]
                [OpenCV]
                [Java AWT]
                [Java Collections]
                [Java IO]
            }
            
            ' MVC relationships
            [MainView] --> [MainController]
            [MazeView] --> [MainController]
            [MazeSelectorView] --> [MainController]
            [MainController] --> [BaseAlgorithm]
            [MainController] --> [MazeGenerator]
            [MainController] --> [MazeGrid]
            
            ' Algorithm implementations
            [DijkstraPriorityQueue] ..|> [BaseAlgorithm]
            [DijkstraArrayList] ..|> [BaseAlgorithm]
            [AStar] ..|> [BaseAlgorithm]
            [AStarOOP] ..|> [BaseAlgorithm]
            
            ' Maze components relationship
            [MazeGenerator] --> [MazeGrid]
            
            ' Technology dependencies
            [MainView] ..> [Java Swing]
            [MazeView] ..> [Java AWT]
            [MazeSelectorView] ..> [Java Swing]
            [MazeGenerator] ..> [OpenCV]
            [BaseAlgorithm] ..> [Java Collections]
            [MazeGenerator] ..> [Java IO]
            @enduml
            """;

    public static final String INVALID_UML = """
             @startuml
            ' Component Diagram
            
            component InputView
            component InputController
            component ASTAnalyzer
            component KnowledgeGraphService
            component GraphDatabaseOperations
            component DiagramManager
            component LLMService
            component GeminiApiClient
            component ClaudeApiClient
            
            InputView  InputController : User Input
            InputController -- ASTAnalyzer : Analyze Code
            ASTAnalyzer -- KnowledgeGraphService : Create Knowledge Graph
            KnowledgeGraphService -- GraphDatabaseOperations : Store/Query Graph
            KnowledgeGraphService -- LLMService : Request Insights
            LLMService -- GeminiApiClient : Gemini API
            LLMService -- ClaudeApiClient : Claude API
            DiagramManager -- KnowledgeGraphService : Query Graph
            InputController -- DiagramManager : Generate Diagram
            InputController -- InputView : Display Results
            @enduml""";

    public static final String THROWS_ILLEGAL_STATE_EXCEPTION_BUT_SYNTAX_STILL_PASS = """
            @startuml
            ' Data Flow Diagram
            
            start
            :User provides input through InputView;
            note right: Step 1\\nUser interacts with the GUI.
            
            :InputController receives input;
            note right: Step 2\\nInputController handles user requests.
            
            :InputController passes input to InputHandler;
            note right: Step 3\\nInputHandler prepares the input for analysis.
            
            :InputHandler processes source code;
            note right: Step 4\\nInputHandler reads files or clones repositories.
            
            :ASTAnalyzer analyzes code and extracts information;
            note right: Step 5\\nASTAnalyzer uses JavaParser to create AST.
            
            fork
            :KnowledgeGraphService creates knowledge graph;
            note right: Step 6\\nKnowledgeGraphService manages graph data.
            
            :KnowledgeGraphService stores graph in Neo4j;
            note right: Step 7\\nGraphDatabaseOperations interacts with Neo4j.
            end fork
            
            fork
            :DiagramManager generates diagrams;
            note right: Step 8\\nDiagramManager uses PlantUML.
            
            :LLMService interacts with LLMs;
            note right: Step 9\\nLLMService uses GeminiApiClient, ClaudeApiClient, PythonClient.
            end fork
            
            :GraphDataToJsonConverter exports graph data to JSON;
            note right: Step 10\\nConverter uses Jackson for serialization.
            
            stop
            
            @enduml""";

    public static final String CORRECT_PLANTUML_SYNTAX_IN_CODE_BLOCK = """
            ```plantuml
            @startuml
            start
            
            :Receive User Input;
            note right: Step 1
            User input received by Controller
            end note
            
            :Pass Input to Chatbot;
            note right: Step 2
            Controller passes input to Chatbot
            end note
            
            :Determine User Intent;
            note right: Step 3
            Chatbot uses IntentDetector
            end note
            
            :Manage Conversation Context;
            note right: Step 4
            ConversationHelper manages context
            end note
            
            fork
              :Retrieve Cryptocurrency Prices;
              note right: Step 5
              If intent involves crypto prices, retrieve data from CoinbasePriceFeed
              end note
            fork again
              :Generate Response;
              note right: Step 6
              Chatbot generates response
              end note
            end fork
            
            :Send Response to Controller;
            note right: Step 7
            Chatbot sends response to Controller
            end note
            
            stop
            @enduml
            ```
            """;
}
