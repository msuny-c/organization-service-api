#!/bin/bash

# Скрипт для сборки frontend и встраивания в backend

echo "Building frontend..."
cd ../organization-frontend
npm ci
npm run build

echo "Copying frontend to backend resources..."
cd ../is
mkdir -p src/main/resources/static
cp -r ../organization-frontend/dist/* src/main/resources/static/

echo "Building backend JAR..."
mvn clean package -DskipTests

echo "Build complete! JAR with frontend is ready in target/"
