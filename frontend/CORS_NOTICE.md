# CORS Configuration Notice

## Problem

When the frontend is served from a **different origin** than the Spring Boot backend, the browser blocks API calls with a CORS error:

```
Access to fetch at 'http://localhost:8080/account/' from origin 'http://localhost:5500' has been blocked by CORS policy.
```

## Solution (Backend — one-time setup)

Add `@CrossOrigin` to both controllers **OR** add a global `CorsConfigurationSource` bean.

### Option A — Per-controller annotation (quickest)

```java
// AccountController.java
@CrossOrigin(origins = "http://localhost:5500")
@RestController
@RequestMapping("/account")
public class AccountController { … }

// PaymentController.java
@CrossOrigin(origins = "http://localhost:5500")
@RestController
@RequestMapping("/payments")
public class PaymentController { … }
```

Use `origins = "*"` to allow all origins during development.

### Option B — Global CORS bean (recommended for production)

Create a new config class:

```java
package com.example.PaymentProcessingSystem.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");   // or specify "http://localhost:5500"
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
```

## Frontend Base URL

The frontend uses `http://localhost:8080` as the backend base URL.
This is configured in `frontend/js/api.js`:

```js
const API_BASE = 'http://localhost:8080';
```

Change this constant if your backend runs on a different port.

## Running the Frontend

1. Start the Spring Boot backend (port 8080).
2. Open `frontend/index.html` in a browser **directly**, or serve with VS Code Live Server / any static file server on port 5500.
3. If using Live Server add the `@CrossOrigin` annotation above to avoid CORS errors.

