# 🎬 AI-Powered Scene Search

> A Netflix-style intelligent video search platform built using **Spring Boot Microservices**, **Apache Kafka**, **FFmpeg**, **Whisper AI**, **Ollama**, **Qdrant**, **MinIO**, and **MySQL**.

Upload any movie or video, automatically generate subtitles, create AI embeddings for every scene, and search scenes using natural language.

---

# 🚀 Features

### 🎥 Video Processing

- Upload videos (MP4, AVI, MOV)
- Store videos in MinIO
- Convert videos to HLS format
- Multiple streaming qualities
    - 1080p
    - 720p
    - 480p
    - 360p

---

### 🎙 Speech Recognition

- Audio extraction using FFmpeg
- Automatic Speech-to-Text
- Subtitle generation (.srt)
- Whisper AI integration

---

### 🧠 AI Scene Search

- Natural Language Search

Example:

```
Where is Harvey Dent?
```

```
Batman enters the cave
```

```
What is the meaning of life?
```

- Semantic Search
- Vector Embeddings
- Confidence Score
- Timestamp Detection
- Instant Video Seeking

---

### ⚡ Event Driven Architecture

- Apache Kafka
- Asynchronous Processing
- Decoupled Microservices
- High Scalability

---

### 📺 Streaming

- HLS Video Streaming
- Adaptive Bitrate
- Fast Loading
- Video Seek Support

---

### 💾 Storage

- MySQL
- MinIO
- Qdrant Vector Database
- Redis (Optional)

---

# 🏗 System Architecture

```
                        +----------------------+
                        |      Frontend        |
                        | HTML + JS + HLS.js   |
                        +----------+-----------+
                                   |
                                   |
                          Spring Cloud Gateway
                                   |
      -------------------------------------------------------------
      |                         |                                |
      |                         |                                |
Video Service           Streaming Service              AI Search Service
      |                                                  |
      |                                                  |
      +---------------- Apache Kafka --------------------+
                              |
                       Encoding Service
                              |
         -----------------------------------------
         |                  |                   |
      FFmpeg             Whisper            Ollama
         |                  |                   |
         |                  |                   |
      HLS Files        Subtitle (.srt)     Embeddings
                              |
                              |
                          Qdrant Vector DB
                              |
                              |
                           Search Result
```

---

# 🔄 Complete Workflow

```
Upload Video
      │
      ▼
Store in MinIO
      │
      ▼
Publish Kafka Event
      │
      ▼
Encoding Service
      │
      ├── HLS Conversion
      ├── Audio Extraction
      └── Whisper Subtitle Generation
      │
      ▼
Subtitle Uploaded to MinIO
      │
      ▼
AI Search Service
      │
      ├── Download Subtitle
      ├── Parse Subtitle
      ├── Generate Embeddings
      ├── Store Metadata
      └── Store Vectors
      │
      ▼
User Searches
      │
      ▼
Generate Query Embedding
      │
      ▼
Qdrant Similarity Search
      │
      ▼
Return Matching Scene + Timestamp
```

---

# 🛠 Tech Stack

| Category | Technology |
|-----------|------------|
| Language | Java 17 |
| Framework | Spring Boot |
| Architecture | Microservices |
| Messaging | Apache Kafka |
| Storage | MinIO |
| Database | MySQL |
| Vector DB | Qdrant |
| AI Model | Ollama |
| Embedding Model | nomic-embed-text |
| Speech Recognition | Whisper |
| Video Processing | FFmpeg |
| Streaming | HLS |
| Frontend | HTML CSS JavaScript |
| Containerization | Docker |

---

# 📦 Microservices

## 1️⃣ API Gateway

- Spring Cloud Gateway

---

## 2️⃣ Video Service

Responsibilities

- Upload Video
- Save Metadata
- Publish Kafka Event

Port

```
8082
```

---

## 3️⃣ Encoding Service

Responsibilities

- Download Video
- FFmpeg Encoding
- Audio Extraction
- Whisper Subtitle Generation

Port

```
8083
```

---

## 4️⃣ Streaming Service

Responsibilities

- Serve HLS Files
- Stream Video

Port

```
8084
```

---

## 5️⃣ AI Search Service

Responsibilities

- Subtitle Parsing
- Embedding Generation
- Qdrant Search
- Semantic Search

Port

```
8085
```

---

# 📁 Project Structure

```
ai-scene-search/

│
├── api-gateway
├── video-service
├── encoding-service
├── streaming-service
├── ai-search-service
│
├── docker-compose.yml
├── README.md
└── docs
```

---

# 📦 Infrastructure

| Service | Port |
|----------|------|
| MySQL | 3306 |
| Kafka | 9092 |
| Zookeeper | 2181 |
| MinIO | 9000 |
| MinIO Console | 9001 |
| Ollama | 11434 |
| Qdrant HTTP | 6333 |
| Qdrant gRPC | 6334 |

---

# 🚀 Installation

Clone Repository

```bash
git clone https://github.com/yourusername/ai-scene-search.git

cd ai-scene-search
```

Start Docker

```bash
docker-compose up -d
```

Pull Embedding Model

```bash
docker exec -it ollama ollama pull nomic-embed-text
```

Build

```bash
mvn clean package
```

Run Services

```bash
java -jar video-service.jar

java -jar encoding-service.jar

java -jar streaming-service.jar

java -jar ai-search-service.jar
```

---

# 🎥 Upload Video

```http
POST /api/v1/videos/upload
```

---

# 🔍 Search Scene

```http
POST /api/v1/search/scenes
```

Example

```json
{
  "query":"Where is Harvey Dent?",
  "movieId":"abc123",
  "limit":5
}
```

---

# 📺 Streaming

```http
GET /api/v1/streaming/{movieId}
```

---

# 📊 Performance

| Operation | Time |
|------------|------|
| Upload | 5-10 sec |
| Encoding | 5-15 min |
| Whisper | 5-10 min |
| Indexing | 1-2 min |
| AI Search | <200 ms |
| Qdrant Query | <50 ms |

---

# 🐳 Docker Services

- MySQL
- Kafka
- Zookeeper
- MinIO
- Ollama
- Qdrant

---

# 📌 Future Improvements

- JWT Authentication
- Spring Security
- Redis Cache
- GPU Whisper
- Kubernetes Deployment
- Elasticsearch Hybrid Search
- OpenTelemetry
- Prometheus
- Grafana
- CI/CD Pipeline
- AI Recommendations
- Face Recognition
- Speaker Identification

---

# 🤝 Contributing

1. Fork Repository
2. Create Feature Branch

```
git checkout -b feature/new-feature
```

3. Commit Changes

```
git commit -m "Added New Feature"
```

4. Push

```
git push origin feature/new-feature
```

5. Create Pull Request

---

# 📜 License

MIT License

---

# ⭐ If you like this project

Give it a ⭐ on GitHub.

---

# 👨‍💻 Author

**Govinda Ekbote**

Java Developer | Spring Boot | Microservices | Kafka | Docker | AI | Vector Search

LinkedIn: https://www.linkedin.com/in/govinda-07/

GitHub: https://github.com/GovindaEkabote/NetFlix