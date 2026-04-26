# Estrategia de Commits por Funcionalidad (Atomic Commits)

Una excelente práctica en el desarrollo de software (especialmente cuando se trabaja con metodologías ágiles o TDD) es realizar **Commits Atómicos**. Esto significa que cada commit debe representar una unidad lógica única y funcional, sin mezclar la configuración del proyecto con la lógica de negocio o los controladores.

A continuación, te presento la recomendación de cómo deberías estructurar tus commits para subir el servicio aislando por capas y responsabilidades. 

Haremos el ejemplo enfocado en **customer-service**, tal y como lo solicitaste:

---

## Ejemplo Práctico: Historial de Commits para `customer-service`

### 📦 Commit 1: Configuración base y dependencias del servicio
**Mensaje propuesto:** `chore(customer): setup spring boot project and base configuration`
*   `customer-service/pom.xml` (Dependencias de Mongo, WebFlux/Web, Eureka Client)
*   `customer-service/src/main/resources/application.yml` (Propiedades, puerto, conexión a mongo)
*   `customer-service/src/main/java/com/ntt/customer/CustomerServiceApplication.java` (Clase Main)
*   `customer-service/.gitignore`

### 🏗️ Commit 2: Modelos de Dominio y DTOs (Contratos)
**Mensaje propuesto:** `feat(customer): add domain models and data transfer objects`
*   `customer-service/src/main/java/com/ntt/customer/model/document/Customer.java`
*   `customer-service/src/main/java/com/ntt/customer/model/enums/CustomerType.java`
*   `customer-service/src/main/java/com/ntt/customer/model/dto/CustomerResponse.java`
*   `customer-service/src/main/java/com/ntt/customer/model/dto/CustomerRequest.java`

### 🗄️ Commit 3: Capa de Acceso a Datos (Repositorio)
**Mensaje propuesto:** `feat(customer): implement mongodb repository interface`
*   `customer-service/src/main/java/com/ntt/customer/repository/CustomerRepository.java`

### 🧪 Commit 4: Pruebas Unitarias y Capa de Servicios (Aproximación TDD)
**Mensaje propuesto:** `feat(customer): implement business logic with unit tests`
*   `customer-service/src/test/java/com/ntt/customer/service/CustomerServiceTest.java` *(Se escribe la prueba)*
*   `customer-service/src/main/java/com/ntt/customer/service/CustomerService.java` *(Interfaz)*
*   `customer-service/src/main/java/com/ntt/customer/service/impl/CustomerServiceImpl.java` *(Implementación para pasar las pruebas)*

### 🚀 Commit 5: Controladores REST y Manejo de Excepciones
**Mensaje propuesto:** `feat(customer): add rest controllers and global error handling`
*   `customer-service/src/main/java/com/ntt/customer/controller/CustomerController.java`
*   `customer-service/src/main/java/com/ntt/customer/exception/GlobalExceptionHandler.java`
*   `customer-service/src/main/java/com/ntt/customer/exception/CustomerNotFoundException.java`

### ⚙️ Commit 6: Pruebas de Integración y validación final (WebTestClient / MockMvc)
**Mensaje propuesto:** `test(customer): add integration tests for rest endpoints`
*   `customer-service/src/test/java/com/ntt/customer/controller/CustomerControllerIT.java` (Pruebas de Integración levantando el contexto de Spring)

---

## 🎯 ¿Por qué te recomiendo esta estructura?

1. **Facilita los Code Reviews (Revisiones de Código):** Tu líder técnico o compañero de equipo no tendrá que revisar 30 archivos en un solo commit. Puede revisar el diseño de los datos en el Commit 2, las reglas de negocio en el Commit 4 y los endpoints en el Commit 5.
2. **Reversibilidad (Rollback):** Si descubres que un Controller tiene un bug catastrófico, puedes revertir solo el Commit 5, manteniendo intactos tus modelos, capa de servicios y configuración.
3. **Se alinea con (TDD):** Escribes tus pruebas unitarias y capa de servicios de la mano antes de exponerlo a la red mediante Controladores.
4. **Mensajes Semánticos:** Fíjate en los mensajes de commit (`feat`, `chore`, `test`). Utilizar **Conventional Commits** (Commits semánticos) da una visibilidad perfecta en el historial de GitHub.
