# Trace-Span-Profiles Example

A complete example setup for testing and developing **span-to-profile correlation** with:
- **Pyroscope** - Continuous profiling
- **Tempo** - Distributed tracing
- **Grafana** - Visualization with trace-to-profile linking
- **OpenTelemetry** - Instrumentation

This setup allows you to test and develop features in:
- [otel-profiling-java](https://github.com/grafana/otel-profiling-java) - OTel extension for span-profile correlation
- [pyroscope-java](https://github.com/grafana/pyroscope-java) - Pyroscope Java agent
- [pyroscope](https://github.com/grafana/pyroscope) - Pyroscope server

## Quick Start

```bash
# Clone this repo
git clone https://github.com/dordor12/trace-span-profiles-example.git
cd trace-span-profiles-example

# Copy and customize environment (optional)
cp .env.example .env

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app
```

**Access the services:**
- **Grafana**: http://localhost:3000 (admin/admin)
- **Pyroscope**: http://localhost:4040
- **Tempo**: http://localhost:3200
- **Demo App**: http://localhost:8080

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Demo App      │────▶│     Tempo       │     │   Pyroscope     │
│ (Spring Boot)   │     │   (Traces)      │     │  (Profiles)     │
│                 │────▶│                 │     │                 │
│ + OTel Agent    │     └────────┬────────┘     └────────┬────────┘
│ + Pyroscope     │              │                       │
│   Agent         │              └───────────┬───────────┘
│ + otel-profiling│                          │
│   extension     │              ┌───────────▼───────────┐
└─────────────────┘              │       Grafana         │
                                 │  (Trace ↔ Profile)    │
                                 └───────────────────────┘
```

## Configuration

All configuration is done via environment variables. Copy `.env.example` to `.env` and customize:

### Docker Images

| Variable | Default | Description |
|----------|---------|-------------|
| `PYROSCOPE_IMAGE` | `grafana/pyroscope:latest` | Pyroscope server image |
| `TEMPO_IMAGE` | `grafana/tempo:2.7.0` | Tempo image |
| `GRAFANA_IMAGE` | `grafana/grafana:11.3.0` | Grafana image |

### Local Development

To use locally built jars instead of published Maven artifacts:

| Variable | Default | Description |
|----------|---------|-------------|
| `PYROSCOPE_OTEL_JAR` | (empty) | Path to local pyroscope-otel.jar |
| `PYROSCOPE_AGENT_JAR` | (empty) | Path to local pyroscope.jar |

### Profiling Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `PROFILING_EVENT` | `wall` | Profile type: `wall`, `cpu`, `alloc`, `lock` |
| `PROFILING_INTERVAL` | `10ms` | Sampling interval |
| `UPLOAD_INTERVAL` | `10s` | Upload frequency to Pyroscope |

### Feature Flags

| Variable | Default | Description |
|----------|---------|-------------|
| `OTEL_PYROSCOPE_CONTEXT_PROPAGATION_ENABLED` | `true` | Enable thread context propagation |
| `OTEL_PYROSCOPE_ROOT_SPAN_ONLY` | `false` | Only profile root spans |
| `OTEL_PYROSCOPE_ADD_SPAN_NAME` | `true` | Include span names in profiles |

## Local Development

### Testing otel-profiling-java Changes

```bash
# 1. Build otel-profiling-java
cd /path/to/otel-profiling-java
./gradlew shadowJar

# 2. Set the path in .env
echo "PYROSCOPE_OTEL_JAR=/path/to/otel-profiling-java/build/libs/pyroscope-otel.jar" >> .env

# 3. Rebuild and restart
docker-compose up -d --build app
```

### Testing pyroscope-java Changes

```bash
# 1. Build pyroscope-java
cd /path/to/pyroscope-java
./gradlew :agent:shadowJar

# 2. Set the path in .env
echo "PYROSCOPE_AGENT_JAR=/path/to/pyroscope-java/agent/build/libs/pyroscope.jar" >> .env

# 3. Rebuild and restart
docker-compose up -d --build app
```

### Testing Pyroscope Server Changes

```bash
# Set a custom Pyroscope image in .env
echo "PYROSCOPE_IMAGE=my-custom-pyroscope:latest" >> .env

# Restart
docker-compose up -d pyroscope
```

### Using the Build Helper Script

```bash
# Build both projects
./scripts/build-local.sh --pyroscope-java /path/to/pyroscope-java \
                         --otel-profiling /path/to/otel-profiling-java
```

## Viewing Span-Profile Correlation

1. Open **Grafana** at http://localhost:3000
2. Go to **Explore** → Select **Tempo**
3. Search for traces (e.g., by service name `demo-app`)
4. Click on a trace to view its spans
5. Click on a span → Click **Profiles** to see the correlated profile

## Demo App Endpoints

The demo app exposes several endpoints for generating load:

| Endpoint | Description |
|----------|-------------|
| `GET /api/users` | List users |
| `GET /api/products` | List products |
| `GET /api/products/search?query=X` | Search products |
| `GET /api/orders` | List orders |
| `GET /api/span-test/worker-spans?count=N&durationMs=M` | Generate N spans on worker threads |

## Troubleshooting

### No profiles showing in Grafana

1. Check Pyroscope is receiving data:
   ```bash
   docker-compose logs pyroscope
   ```

2. Check the app is sending profiles:
   ```bash
   docker-compose logs app | grep -i pyroscope
   ```

### Span-profile correlation not working

1. Verify `otel-profiling-java` extension is loaded:
   ```bash
   docker-compose logs app | grep -i "pyroscope-otel"
   ```

2. Check context propagation is enabled:
   ```bash
   docker-compose logs app | grep -i "context.propagation"
   ```

### Building the demo app fails

Ensure you have Docker with BuildKit enabled:
```bash
export DOCKER_BUILDKIT=1
docker-compose build app
```

## Contributing

1. Fork this repository
2. Make your changes
3. Test with local jars
4. Submit a pull request

## Related Projects

- [otel-profiling-java](https://github.com/grafana/otel-profiling-java) - OTel extension for span-profile correlation
- [pyroscope-java](https://github.com/grafana/pyroscope-java) - Pyroscope Java agent
- [pyroscope](https://github.com/grafana/pyroscope) - Pyroscope server
- [Grafana Tempo](https://github.com/grafana/tempo) - Distributed tracing backend

## License

Apache 2.0
