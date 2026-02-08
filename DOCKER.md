# Docker Deployment Guide

## Quick Start

### Using Docker Compose (Easiest)
```bash
# Build and run with one command
docker-compose up --build

# Access the app at http://localhost:8080
```

### Using Docker CLI

**Build the image:**
```bash
docker build -t fileconverter:latest .
```

**Run the container:**
```bash
docker run -d \
  -p 8080:8080 \
  -v fileconverter-storage:/app/storage \
  -e file.converter.libreoffice.enabled=true \
  -e file.storage.location=/app/storage \
  --name fileconverter \
  fileconverter:latest
```

**View logs:**
```bash
docker logs -f fileconverter
```

**Stop the container:**
```bash
docker stop fileconverter
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `file.converter.libreoffice.enabled` | `true` | Enable LibreOffice conversion |
| `file.converter.libreoffice.path` | `soffice` | Path to soffice executable |
| `file.storage.location` | `/app/storage` | Directory to store uploaded/converted files |
| `file.storage.retention-days` | `30` | Days to retain files before cleanup |

## What's Included

- **Multi-stage build:** Reduces final image size by only including runtime dependencies
- **LibreOffice:** Pre-installed for high-fidelity Office→PDF conversion
- **Fonts:** DejaVu fonts included for better text rendering
- **Health checks:** Automatic container restart if app becomes unhealthy
- **Persistent storage:** Named volume preserves converted files across restarts

## Deployment to Cloud

### Azure Container Instances (ACI)
```bash
# Tag the image
docker tag fileconverter:latest myregistry.azurecr.io/fileconverter:latest

# Push to Azure Container Registry
docker push myregistry.azurecr.io/fileconverter:latest

# Deploy via Azure CLI
az container create \
  --resource-group myRG \
  --name fileconverter \
  --image myregistry.azurecr.io/fileconverter:latest \
  --ports 8080 \
  --cpu 1 --memory 2
```

### Docker Hub
```bash
docker tag fileconverter:latest myusername/fileconverter:latest
docker push myusername/fileconverter:latest
```

### Kubernetes
See [kubernetes.yaml](kubernetes.yaml) for a sample Kubernetes deployment manifest.

## Troubleshooting

**"LibreOffice not found":**
```bash
# Check if LibreOffice is running in the container
docker exec fileconverter /usr/bin/soffice --version
```

**Conversion failures:**
```bash
# View detailed logs
docker logs fileconverter | grep -i "error\|warning"
```

**Out of storage:**
```bash
# Check storage usage
docker exec fileconverter du -sh /app/storage

# Clean up old files by restarting
docker-compose restart
```
