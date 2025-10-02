FROM openjdk:17-jdk-slim

# Instalar Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Directorio de trabajo
WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml .
COPY src/ src/
COPY data/ data/
COPY logs/ logs/

# Compilar la aplicación
RUN mvn clean compile

# Exponer puertos si es necesario
EXPOSE 8080

# Comando por defecto
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.inventario.Main"]
