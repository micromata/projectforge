In progress

Dirctories:
project/
├── ProjectForge/          # Application
├── ProjectForge/database  # Database
├── nginx/                 # Nginx configuration
├── docker-compose.yml     # Docker Compose setup
└── .env                   # Evirnoment variables


Edit .env, ./nginx/nginx.conf

Deploy SSL certificates

For example, use Let’s Encrypt with Certbot to generate SSL certificates and store them in nginx/certs and nginx/keys.

Start the application with:
docker-compose up -d

Check running containers:
docker ps
docker-compose logs


