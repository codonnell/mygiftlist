# Configuration

For cost reasons, we are planning on deploying staging and prod environments in the same solo topology datomic cluster. We will also support a dev environment which does not serve requests via API Gateway and instead uses a local server. These three environments will need configuration to distinguish:
* Auth0 tenant
* Database name
* Logging configuration?

Maybe we just won't have a staging environment. Then we just need to differentiate dev and production. A kind of grody way to do this would be to create an atom in `src` with the value `:prod` and then `reset!` it to `:dev` in `dev/user.clj`. We could then use this value to dispatch on getting config.

# Domains

We want to serve static assets via CloudFront and S3. We want to serve API requests via API Gateway. It would be great to be able to add an origin for API Gateway into the CloudFront distribution. That way we can serve API requests from the same domain, which will work well locally and in production.

Probably easiest for local development to not use nginx and just serve all assets via httpkit server. However, this means we need to separate the handler bits that serve static assets from those that respond to API requests, since we don't want to deploy a handler that serves static assets.
