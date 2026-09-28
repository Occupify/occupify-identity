**Occupify** is a technology-focused social network and freelance marketplace that connects applicants with employers. The platform allows applicants to discover and apply for projects based on their skills and preferences, while employers can create projects, search for suitable applicants, and manage the hiring process.

The system is designed using a **microservices architecture**, with separate services responsible for identity and account management, marketplace operations, contracts and payments, notifications, API routing, and administration. This separation allows each service to manage its own business responsibilities and data while communicating with other services through well-defined APIs and events.

| Service            | Description                                                                                                                                               |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Identity**       | Entry point of the system. Routes client requests to the appropriate service and handles common concerns such as authentication, CORS, and rate limiting.. Manages authentication and user accounts, including registration, login, roles, profiles, CVs, and account settings.                                      |
| **Core**           | Handles the main marketplace business logic: applicants search/filter projects, employers create/manage projects, applications, and reviews & ratings.    |
| **Payment**        | Manages contracts, user wallets, payment transactions, and contract-related financial operations.                                                         |
| **Notification**   | Handles system notifications such as email and in-app notifications triggered by user or system events.                                                   |
| **Administration** | Provides administrative functions such as audit logs, reports, and system/user management.                                                                |




| Service            | Responsibility                                                                                     | Main data                                     | Suggested App Port |     Database Port |
| ------------------ | -------------------------------------------------------------------------------------------------- | --------------------------------------------- | -----------------: | ----------------: |
| **Identity**        |Gateway, single entry point, routing, auth propagation, rate limiting                                       | None                                          |             `8181` |PostgreSQL `5530`
| **Core**           | Projects, applications, applicant search/filtering, employer project management, reviews & ratings | Projects, applications/bids, reviews, ratings |             `8182` | PostgreSQL `5531` |
| **Payment**        | Contracts, contract lifecycle, wallet, transactions/payment events                                 | Contracts, wallets, transactions              |             `8183` | PostgreSQL `5532` |
| **Notification**   | Email/in-app notifications                                                                         | Notifications, templates, delivery status     |             `8184` | PostgreSQL `5533` |
| **Administration** | Admin operations, reports, audit logs                                                              | Audit logs, reports/admin data                |             `8185` | PostgreSQL `5534` |

```
Client
   |
   v
Identity :8181
   |
   +----> Core :8182
   |
   +----> Payment :8183
   |
   +----> Notification :8184
   |
   +----> Administration :8185
```
