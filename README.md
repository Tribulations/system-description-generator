# System Description Generator

- This README explains how to run the System Description Generator (SDG)
- See [HOW-TO.md](HOW-TO.md) for a quick start guide and how to use it.

## Setup Instructions

To run the System Description Generator (SDG), follow these steps:

1. **Obtain an LLM API key**
    - Choose either [Anthropic (Claude)](https://www.anthropic.com/api) (paid) or [Google Gemini](https://ai.google.dev/) (free)
    - Create an account and generate your API key

2. **Configure required environment variables**
    - Create a .env file based on the [env.template](.env.template) file in the root directory of the project.

3. **Install Graphviz**
    - See [Graphviz](https://www.graphviz.org/download/)
    - Used for rendering diagrams

4. **Setup Neo4j database (knowledge graph)**
    - You can either install and configure Neo4j locally or use a docker container (likely easier on Windows but interactions with database are noticeably slower)
      - **Install Neo4j locally**: See the [Neo4j installation section](#neo4j-installation-and-configuration) below for instructions
      - **Use Neo4j docker container**
        - Install [docker compose](https://docs.docker.com/compose/install) if not already installed 
        - Run `docker compose --env-file .env up` in the root directory of the project or start using docker desktop.

5. **Build and run**
    - Run `mvn clean install` to build the application
      - If the integration tests fail, run `mvn clean install -DskipTests` to skip the integration tests
    - Run the SDG application with `java -jar target/system-description-generator-1.0-SNAPSHOT.jar`
    - The application should start!

## Neo4j installation and configuration

### Query the database in browser interface

Neo4j browser interface url: http://localhost:7474

In the browser interface the following query can be used to query all data for display in table, text, code, or graph:

```bash
MATCH (n)-[r]->(m) RETURN *
```

### Installation on Ubuntu

1. First, create the installation directory and download the Neo4j key:

```bash
# Create directory for Neo4j installation files
mkdir -p neo4j_install
cd neo4j_install
wget https://debian.neo4j.com/neotechnology.gpg.key
cd ..
```

2. Create an installation script e.g., `install-neo4j-ubuntu.sh` and add following content:

```bash
# Content of install-neo4j-ubuntu.sh
# Create directory for Neo4j installation files, change directory and download the Neo4j key
mkdir -p neo4j_install && cd neo4j_install && wget https://debian.neo4j.com/neotechnology.gpg.key

# Add the key to apt
sudo apt-key add neo4j_install/neotechnology.gpg.key

# Add the Neo4j repository
echo 'deb https://debian.neo4j.com stable latest' | sudo tee /etc/apt/sources.list.d/neo4j.list

# Update package list
sudo apt-get update

# Install Neo4j
sudo apt-get install neo4j

# Start Neo4j service
sudo systemctl start neo4j

# Enable Neo4j to start on boot
sudo systemctl enable neo4j
```

3. Make the script executable (if necessary), and run it:

```bash
chmod +x install-neo4j-ubuntu.sh
./install-neo4j-ubuntu.sh
```

4. Initial Database Setup To set up the database first time, Exec: `cypher-shell -u neo4j -p neo4j`

To enter neo4j shell exec: `cypher-shell -u 'neo4j' -p 'password'` where -u sets the user and -p the password

5. Verify the installation:

```bash
# Check Neo4j service status
sudo systemctl status neo4j

# Check Neo4j version
neo4j --version
```

### Data Directories

Data directory: Default location: /var/lib/neo4j/data Logs: /var/log/neo4j

### Ports used by Neo4j

- 7474: HTTP port for Neo4j Browser 
- 7687: Bolt port for database connections (used by the Java application)

### Installation on Windows (TODO)

## Logging

Logging levels and other settings can be set in `src/main/resources/logback.xml`

## Python Microservice & Java Client (Currently not used by the application)

### Install Python modules and dependencies (Currently not used by the application)

```bash
pip install -r src/main/python/requirements.txt
```

### Run the python service (Currently not used by the application)

```bash
python src/main/python/microservice/multiply_service.py
```

## Miscellaneous

### Download java doc and source documentation

```bash
mvn dependency:sources dependency:resolve -Dclassifier=javadoc
