# Sea. Desafio Backend Pleno/Senior

API REST para um sistema de Solicitações de Atendimento, desenvolvida em Java 21 com Spring Boot 4. O projeto implementa cadastro multi step de solicitações, controle de acesso por perfil (CLIENT, ANALYST, ADMIN), integração com a API pública ViaCEP, indexação e busca via Elasticsearch, autenticação JWT e auditoria via AOP.

## Sumário

1. Stack utilizada
2. Arquitetura e organização do código
3. Perfis de acesso e regras de negócio
4. Como subir o projeto (Docker Compose)
5. Variáveis de ambiente
6. Criação do usuário ADMIN inicial
7. Documentação da API (Swagger)
8. Fluxo de uso passo a passo (curl)
9. Collection do Postman
10. Endpoints disponíveis
11. Busca no Elasticsearch
12. Auditoria (AOP)
13. Tratamento de erros
14. Testes
15. Observabilidade (Actuator e Prometheus)

## 1. Stack utilizada

- Java 21
- Spring Boot 4.1.1 (Web, Data JPA, Security, Validation, Actuator)
- PostgreSQL 16 (persistência transacional)
- Flyway (migração de schema)
- Elasticsearch 9.5.2 (indexação e busca)
- JWT (java-jwt) para autenticação
- Spring AOP para auditoria
- ViaCEP para consulta de endereço por CEP
- springdoc-openapi para documentação Swagger
- Micrometer + Prometheus para métricas
- Docker e Docker Compose
- JUnit 5, Spring Security Test, Testcontainers/integração para os testes

## 2. Arquitetura e organização do código

O código segue separação em camadas dentro de `src/main/java/com/desafio/sea`:

```
controller/     endpoints REST (Auth, Admin, Solicitation, Analyst)
service/        regras de negócio
repository/     acesso a dados via Spring Data JPA
domain/         entidades JPA, enums e DTOs (records)
infra/security/ configuração de segurança, filtro JWT e geração/validação de token
infra/aspect/   anotação @Audit e aspecto de auditoria
infra/client/   integração com a API ViaCEP
infra/elasticsearch/ documento, repositório, query builder e serviço de indexação
infra/config/   configuração do Elasticsearch, OpenAPI e o seed do usuário ADMIN
```

Os DTOs de request são `records` com validação via Bean Validation. As respostas de erro seguem o padrão `ProblemDetail` (RFC 7807).

## 3. Perfis de acesso e regras de negócio

### CLIENT
- Se registra sozinho em `POST /api/auth/register`.
- Cria e edita apenas as próprias solicitações.
- Preenche a solicitação em três etapas (step1, step2, step3), podendo salvar e continuar depois.
- Só edita enquanto o status da solicitação é `DRAFT`.
- Envia a solicitação para análise em `POST /api/solicitations/{id}/submit`, que valida a completude de todas as etapas antes de mudar o status para `SUBMITTED`.

### ANALYST
- Criado exclusivamente pelo ADMIN.
- Só enxerga e analisa solicitações cujo estado (UF) esteja dentro da sua cobertura, configurada pelo ADMIN.
- Pode iniciar a análise (`SUBMITTED` para `IN_REVIEW`) e decidir (`APPROVE` ou `REJECT`).
- Não altera os dados preenchidos pelo cliente.

### ADMIN
- Único perfil que cria usuários internos (`ANALYST`).
- Define e atualiza a cobertura de UFs de cada analista.
- Lista todos os usuários e ativa/desativa contas.

Observação sobre o controle de acesso implementado: as rotas `/api/admin/**` exigem `ROLE_ADMIN` e as rotas `/api/analyst/**` exigem `ROLE_ANALYST`, de forma exclusiva por perfil.

### Regras de validação por etapa

**Step 1**
- `serviceType`: obrigatório, um de `INSTALLATION`, `MAINTENANCE`, `INSPECTION`
- `title`: obrigatório, entre 3 e 80 caracteres
- `description`: obrigatória, entre 20 e 1000 caracteres

**Step 2**
- `cep`: obrigatório, formato `00000-000` ou 8 dígitos
- `number`: obrigatório, entre 1 e 20 caracteres
- `complement`: opcional, até 100 caracteres
- Ao informar o CEP, o backend consulta a ViaCEP e preenche automaticamente `street`, `neighborhood`, `city` e `state`
- Se o CEP for inválido ou a consulta falhar, a etapa não é concluída

**Step 3**
- `priority`: obrigatório, um de `LOW`, `MEDIUM`, `HIGH`
- `preferredDate`: obrigatória, não pode estar no passado
- `estimatedValue`: obrigatório, maior ou igual a zero
- `termsAccepted`: obrigatório, precisa ser `true`
- Regra adicional: se `priority = HIGH`, `estimatedValue` deve ser maior ou igual a 100

**Submit**
- Revalida a completude das três etapas
- Só o CLIENT dono da solicitação pode submeter, e apenas enquanto o status é `DRAFT`
- Ao concluir, define `status = SUBMITTED`, preenche `submittedAt` e bloqueia novas edições pelo cliente

**Decisão do analista**
- Só é permitida quando o status é `SUBMITTED` ou `IN_REVIEW`
- `decision`: `APPROVE` ou `REJECT`
- `comment`: obrigatório, entre 10 e 1000 caracteres
- Ao decidir, preenche `analysisComment`, `analyzedBy` e `analyzedAt`

## 4. Como subir o projeto (Docker Compose)

Pré requisitos: Docker e Docker Compose instalados.

Passo a passo:

```bash
git clone https://github.com/MikaelRegetz10/desafio-pleno-sea.git
cd desafio-pleno-sea
cp .env.example .env
```

Edite o arquivo `.env` e ajuste os valores conforme necessário (principalmente `JWT_SECRET`, `DB_PASSWORD` e `ADMIN_PASSWORD`).

Suba os serviços:

```bash
docker compose up --build
```

Isso sobe três containers:
- `solicitation_postgres`: banco de dados PostgreSQL na porta definida em `DB_PORT`
- `solicitation_elasticsearch`: Elasticsearch na porta 9200
- `solicitation_app`: a aplicação Spring Boot na porta definida em `SERVER_PORT` (padrão 8080)

A aplicação aguarda o Postgres e o Elasticsearch ficarem saudáveis (healthcheck) antes de iniciar. As migrações do Flyway rodam automaticamente na inicialização, criando o schema do banco. Não é necessário nenhum comando manual de migração.

Para derrubar os containers:

```bash
docker compose down
```

Para derrubar e apagar os volumes de dados (banco e índice zerados):

```bash
docker compose down -v
```

### Rodando localmente sem Docker (opcional)

Caso queira rodar a aplicação fora do container, mantendo Postgres e Elasticsearch em Docker:

```bash
docker compose up -d postgres elasticsearch
./mvnw spring-boot:run
```

Nesse caso, garanta que as variáveis de ambiente do `.env` estejam exportadas no shell ou configuradas na IDE, com `DB_HOST=localhost` e `ELASTICSEARCH_HOST=http://localhost:9200`.

## 5. Variáveis de ambiente

Definidas em `.env` (veja `.env.example` como referência):

| Variável | Descrição | Exemplo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Profile ativo do Spring | `dev` |
| `SERVER_PORT` | Porta exposta pela aplicação | `8080` |
| `DB_HOST` | Host do PostgreSQL | `localhost` ou `postgres` (dentro do compose) |
| `DB_PORT` | Porta do PostgreSQL | `5432` |
| `DB_NAME` | Nome do banco | `solicitation_db` |
| `DB_USER` | Usuário do banco | `postgres` |
| `DB_PASSWORD` | Senha do banco | definir |
| `ELASTICSEARCH_HOST` | URL do Elasticsearch | `http://localhost:9200` |
| `JWT_SECRET` | Segredo usado para assinar o token JWT | definir uma chave forte |
| `JWT_EXPIRATION` | Tempo de expiração do token em milissegundos | `86400000` |
| `ADMIN_EMAIL` | E-mail do usuário ADMIN criado automaticamente | `admin@sea.com` |
| `ADMIN_PASSWORD` | Senha do usuário ADMIN criado automaticamente | definir |

## 6. Criação do usuário ADMIN inicial

O usuário ADMIN não precisa ser criado manualmente. Ao subir a aplicação, a classe `DataInitializer` verifica se já existe um usuário com o e-mail definido em `ADMIN_EMAIL`; se não existir, cria automaticamente um usuário com perfil `ADMIN` usando `ADMIN_EMAIL` e `ADMIN_PASSWORD` do `.env`.

Com os valores padrão do exemplo, o login do administrador é feito com o e-mail e a senha definidos nessas duas variáveis.

## 7. Documentação da API (Swagger)

Com a aplicação em execução, a documentação interativa fica disponível em:

```
http://localhost:8080/swagger-ui.html
```

O contrato OpenAPI em JSON fica em:

```
http://localhost:8080/v3/api-docs
```

## 8. Fluxo de uso passo a passo (curl)

Todas as rotas ficam sob o context path `/api`.

### 8.1 Login como ADMIN

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@sea.com", "password": "SUA_SENHA_ADMIN"}'
```

A resposta traz o token JWT. Exporte para facilitar os próximos comandos:

```bash
export ADMIN_TOKEN="token_recebido_no_login"
```

### 8.2 ADMIN cria um analista

```bash
curl -X POST http://localhost:8080/api/admin/analyst \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "João Silva",
    "email": "joao.silva@sea.com",
    "password": "123456",
    "coverageStates": ["SP", "RJ", "MG"]
  }'
```

### 8.3 Cliente se registra e faz login

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Cliente Teste", "email": "cliente@teste.com", "password": "123456"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "cliente@teste.com", "password": "123456"}'

export CLIENT_TOKEN="token_recebido_no_login"
```

### 8.4 Cliente preenche a solicitação em três etapas

```bash
curl -X POST http://localhost:8080/api/solicitations/step1 \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceType": "INSTALLATION",
    "title": "Instalação de Painel Solar",
    "description": "Instalação completa de módulos fotovoltaicos no telhado."
  }'
```

Guarde o `id` retornado:

```bash
export SOLICITATION_ID="id_retornado"

curl -X PUT http://localhost:8080/api/solicitations/$SOLICITATION_ID/step2 \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"cep": "70040-900", "number": "100", "complement": "Bloco A"}'

curl -X PUT http://localhost:8080/api/solicitations/$SOLICITATION_ID/step3 \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "priority": "HIGH",
    "preferredDate": "2026-09-15",
    "estimatedValue": 1500.00,
    "termsAccepted": true
  }'
```

### 8.5 Cliente submete a solicitação

```bash
curl -X POST http://localhost:8080/api/solicitations/$SOLICITATION_ID/submit \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

### 8.6 Analista busca, inicia análise e decide

```bash
export ANALYST_TOKEN="token_do_login_do_analista"

curl -G http://localhost:8080/api/analyst/solicitations/search \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  --data-urlencode "q=solar" \
  --data-urlencode "page=0" \
  --data-urlencode "size=10"

curl -X POST http://localhost:8080/api/analyst/solicitations/$SOLICITATION_ID/start \
  -H "Authorization: Bearer $ANALYST_TOKEN"

curl -X POST http://localhost:8080/api/analyst/solicitations/$SOLICITATION_ID/decide \
  -H "Authorization: Bearer $ANALYST_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "decision": "APPROVE",
    "comment": "Solicitação analisada e aprovada conforme requisitos técnicos."
  }'
```

## 9. Collection do Postman

O repositório inclui uma collection do Postman com os principais fluxos (autenticação, administração, solicitação multi step e análise). As variáveis de coleção `base_url`, `token`, `solicitation_id` e `user_id` já cobrem o encadeamento entre requisições; os testes de script preenchem `token` automaticamente após cada login e `solicitation_id` após a criação do step 1.

Para usar, importe o arquivo da collection no Postman que se encontra na pasta postman na raiz do projeto, ajuste `base_url` se necessário (padrão `http://localhost:8080`) e execute as requisições de login antes das demais chamadas autenticadas.

## 10. Endpoints disponíveis

### Autenticação (`/api/auth`)
| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/auth/register` | público | Cria um usuário CLIENT |
| POST | `/auth/login` | público | Autentica e retorna o token JWT |

### Administração (`/api/admin`, requer ROLE_ADMIN)
| Método | Rota | Descrição |
|---|---|---|
| POST | `/admin/analyst` | Cria um usuário ANALYST com estados de cobertura |
| PUT | `/admin/{id}/coverage` | Atualiza a lista de UFs cobertas por um analista |
| GET | `/admin/users` | Lista todos os usuários cadastrados |
| PATCH | `/admin/{id}/toggle-status` | Ativa ou desativa um usuário |

### Solicitações (`/api/solicitations`, requer autenticação como CLIENT)
| Método | Rota | Descrição |
|---|---|---|
| POST | `/solicitations/step1` | Cria a solicitação (rascunho) com os dados da etapa 1 |
| PUT | `/solicitations/{id}/step1` | Atualiza os dados da etapa 1 |
| PUT | `/solicitations/{id}/step2` | Atualiza o endereço (etapa 2), consultando a ViaCEP |
| PUT | `/solicitations/{id}/step3` | Atualiza os dados finais (etapa 3) |
| GET | `/solicitations/{id}` | Consulta a solicitação pertencente ao cliente autenticado |
| POST | `/solicitations/{id}/submit` | Envia a solicitação para análise |

### Análise (`/api/analyst`, requer ROLE_ANALYST)
| Método | Rota | Descrição |
|---|---|---|
| GET | `/analyst/solicitations/search` | Busca paginada e filtrada via Elasticsearch |
| GET | `/analyst/solicitations/{id}` | Consulta detalhes de uma solicitação dentro da cobertura |
| POST | `/analyst/solicitations/{id}/start` | Move a solicitação de `SUBMITTED` para `IN_REVIEW` |
| POST | `/analyst/solicitations/{id}/decide` | Registra a decisão (`APPROVE` ou `REJECT`) |

## 11. Busca no Elasticsearch

A entidade `Solicitation` é indexada a cada criação, atualização de etapa, submissão e decisão. O documento indexado contém `id`, `clientId`, `status`, `serviceType`, `title`, `description`, `state`, `city`, `priority`, `createdAt` e `submittedAt`.

O endpoint `GET /api/analyst/solicitations/search` aceita os parâmetros:

- `q`: busca textual em `title` e `description`
- `status`: um ou mais status (`DRAFT`, `SUBMITTED`, `IN_REVIEW`, `APPROVED`, `REJECTED`)
- `serviceType`: filtro opcional
- `priority`: filtro opcional
- `state`: filtro opcional; para o perfil ANALYST, o backend sempre restringe o resultado aos estados de cobertura do analista autenticado, mesmo que o parâmetro seja enviado com outro valor
- `dateFrom` e `dateTo`: intervalo sobre `submittedAt`
- `page` e `size`: paginação
- `sort`: ordenação, padrão `submittedAt desc`

A resposta segue o formato `{ items, page, size, total }`.

## 12. Auditoria (AOP)

A anotação `@Audit` é aplicada nos endpoints críticos:

- `POST /solicitations/{id}/submit`
- `POST /analyst/solicitations/{id}/decide`
- `POST /admin/analyst`

Um aspecto (`AuditAspect`) intercepta essas chamadas e grava um registro na tabela `audit_logs` com `userId`, `role`, `action`, `entityId`, `durationMs` e o resultado (sucesso ou erro, com a mensagem de erro quando aplicável).

## 13. Tratamento de erros

Os erros seguem o padrão `ProblemDetail` (RFC 7807), com `title`, `detail`, `type`, `status`, `timestamp` e, no caso de erros de validação, um mapa `invalidFields` com o campo e a respectiva mensagem.

| Situação | Status HTTP |
|---|---|
| Erro de validação de campos | 400 |
| Regra de negócio violada (ex.: submit com etapas incompletas) | 400 ou 409 |
| Acesso negado (fora da cobertura ou role incorreta) | 403 |
| Erro interno não tratado | 500 |

## 14. Testes

O projeto conta com testes unitários das regras de negócio (submissão, análise, busca, validação de DTOs) e testes de integração do fluxo completo de solicitação.

Para rodar os testes:

```bash
./mvnw test
```

## 15. Observabilidade (Actuator e Prometheus)

Os endpoints do Actuator estão expostos publicamente para facilitar a avaliação:

```
http://localhost:8080/actuator/health
http://localhost:8080/actuator/info
http://localhost:8080/actuator/metrics
http://localhost:8080/actuator/prometheus
```
