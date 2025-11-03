#!/bin/bash

echo "Building frontend for Spring Boot deployment..."

cd frontend
pnpm build

if [ $? -eq 0 ]; then
    echo "Frontend built successfully!"
    echo "Files are now available in src/main/resources/static/"
    echo "You can now start the Spring Boot application."
else
    echo "Frontend build failed!"
    exit 1
fi
