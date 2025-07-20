# ⚡ Stack de Orquestación - Apache Airflow

## 🎯 Descripción General

El **docker-compose-workflow.yml** implementa una plataforma completa de orquestación de workflows usando Apache Airflow. Proporciona programación, monitoreo y gestión de pipelines de datos, tareas ETL y procesos de negocio automatizados.

## 🏗️ Servicios Incluidos

### 🌊 **Airflow Core**
- **Airflow Webserver** - Interfaz web y API
- **Airflow Scheduler** - Programador de tareas
- **Airflow Init** - Inicialización y migración de BD

### 💾 **Supporting Services**
- **Airflow PostgreSQL** - Base de datos de metadatos
- **Redis** - Broker para Celery executor

## 🔌 Puertos Expuestos

| Servicio | Puerto | Protocolo | Descripción |
|----------|--------|-----------|-------------|
| Airflow Webserver | 8086 | HTTP | Interfaz web y API |
| Airflow PostgreSQL | 5436 | TCP | Base de datos |
| Redis | 6379 | TCP | Message broker |

## 🔑 Accesos por Defecto

### Airflow Web UI
- **URL:** http://localhost:8086
- **Usuario:** admin
- **Password:** admin123
- **API:** http://localhost:8086/api/v1/

### Airflow Database
- **Host:** localhost:5436
- **Usuario:** airflow
- **Password:** airflow123
- **Base de datos:** airflow

### Redis
- **Host:** localhost:6379
- **No requiere autenticación**

## ⚙️ Variables de Entorno Requeridas

```bash
# Airflow Core
AIRFLOW_ADMIN_USER=admin
AIRFLOW_ADMIN_PASSWORD=admin123
AIRFLOW_ADMIN_EMAIL=admin@empresa.com

# Database
AIRFLOW_DB_USER=airflow
AIRFLOW_DB_PASSWORD=airflow123
AIRFLOW_DB_NAME=airflow

# Executor Configuration
AIRFLOW__CORE__EXECUTOR=CeleryExecutor
AIRFLOW__CELERY__BROKER_URL=redis://redis:6379/0
AIRFLOW__CELERY__RESULT_BACKEND=db+postgresql://airflow:airflow123@airflow-postgres:5432/airflow

# Security
AIRFLOW__CORE__FERNET_KEY=airflow123456789012345678901234567890123456789012345

# Features
AIRFLOW__CORE__LOAD_EXAMPLES=false
AIRFLOW__CORE__DAGS_ARE_PAUSED_AT_CREATION=true
AIRFLOW__API__AUTH_BACKENDS=airflow.api.auth.backend.basic_auth,airflow.api.auth.backend.session

# Resource Limits
CPU_LIMIT_LARGE=1.0
MEMORY_LIMIT_LARGE=1g
```

## 🚀 Casos de Uso

### 1. **DAG Básico - ETL Diario**

#### DAG para sincronización de datos
```python
# dags/productos_etl_dag.py
from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.postgres_operator import PostgresOperator
from airflow.operators.bash import BashOperator

default_args = {
    'owner': 'data-team',
    'depends_on_past': False,
    'start_date': datetime(2024, 1, 1),
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 2,
    'retry_delay': timedelta(minutes=5)
}

dag = DAG(
    'productos_etl_daily',
    default_args=default_args,
    description='ETL diario de productos',
    schedule_interval='0 2 * * *',  # 2 AM diario
    catchup=False,
    max_active_runs=1,
    tags=['etl', 'productos', 'daily']
)

def extract_productos():
    """Extraer productos desde PostgreSQL"""
    import psycopg2
    import pandas as pd
    
    conn = psycopg2.connect(
        host='postgres',
        database='productos_db',
        user='admin',
        password='admin123'
    )
    
    query = """
    SELECT id, nombre, precio, categoria, updated_at
    FROM productos 
    WHERE updated_at >= CURRENT_DATE - INTERVAL '1 day'
    """
    
    df = pd.read_sql(query, conn)
    df.to_csv('/tmp/productos_extract.csv', index=False)
    conn.close()
    
    return f"Extraídos {len(df)} productos"

def transform_productos():
    """Transformar y limpiar datos"""
    import pandas as pd
    
    df = pd.read_csv('/tmp/productos_extract.csv')
    
    # Limpiar datos
    df['nombre'] = df['nombre'].str.strip().str.title()
    df['precio'] = pd.to_numeric(df['precio'], errors='coerce')
    
    # Filtrar productos válidos
    df = df[df['precio'] > 0]
    
    # Agregar campos calculados
    df['precio_categoria'] = df.groupby('categoria')['precio'].transform('mean')
    df['is_premium'] = df['precio'] > df['precio_categoria'] * 1.5
    
    df.to_csv('/tmp/productos_transformed.csv', index=False)
    return f"Transformados {len(df)} productos"

def load_to_mongodb():
    """Cargar datos transformados a MongoDB"""
    import pandas as pd
    from pymongo import MongoClient
    
    df = pd.read_csv('/tmp/productos_transformed.csv')
    
    client = MongoClient('mongodb://mongo:27017/')
    db = client['productos_db']
    collection = db['productos_analytics']
    
    # Convertir a dict y insertar
    records = df.to_dict('records')
    result = collection.insert_many(records)
    
    client.close()
    return f"Cargados {len(result.inserted_ids)} productos"

# Tasks
extract_task = PythonOperator(
    task_id='extract_productos',
    python_callable=extract_productos,
    dag=dag
)

transform_task = PythonOperator(
    task_id='transform_productos',
    python_callable=transform_productos,
    dag=dag
)

load_task = PythonOperator(
    task_id='load_to_mongodb',
    python_callable=load_to_mongodb,
    dag=dag
)

# Verificación de calidad
quality_check = PostgresOperator(
    task_id='quality_check',
    postgres_conn_id='postgres_default',
    sql="""
    SELECT COUNT(*) as total_productos,
           COUNT(CASE WHEN precio > 0 THEN 1 END) as productos_validos,
           AVG(precio) as precio_promedio
    FROM productos
    WHERE updated_at >= CURRENT_DATE - INTERVAL '1 day'
    """,
    dag=dag
)

# Notificación final
notify_task = BashOperator(
    task_id='notify_completion',
    bash_command="""
    curl -X POST http://slack-webhook-url \
    -H 'Content-Type: application/json' \
    -d '{"text": "ETL de productos completado exitosamente"}'
    """,
    dag=dag
)

# Dependencias
extract_task >> transform_task >> load_task >> quality_check >> notify_task
```

### 2. **DAG Complejo - Pipeline de Machine Learning**

#### ML Pipeline con validación y deployment
```python
# dags/ml_pipeline_dag.py
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.bash import BashOperator
from airflow.sensors.filesystem import FileSensor
from airflow.providers.docker.operators.docker import DockerOperator

def prepare_training_data(**context):
    """Preparar datos para entrenamiento"""
    import pandas as pd
    from sklearn.model_selection import train_test_split
    
    # Extraer datos de múltiples fuentes
    # ... código de preparación ...
    
    return {'dataset_size': len(df), 'features': df.columns.tolist()}

def train_model(**context):
    """Entrenar modelo ML"""
    from sklearn.ensemble import RandomForestClassifier
    from sklearn.metrics import accuracy_score
    import joblib
    
    # Cargar datos y entrenar
    # ... código de entrenamiento ...
    
    # Guardar modelo
    joblib.dump(model, '/models/productos_classifier.pkl')
    
    return {'accuracy': accuracy, 'model_path': '/models/productos_classifier.pkl'}

def validate_model(**context):
    """Validar modelo contra conjunto de test"""
    # ... validación ...
    
    if accuracy < 0.85:
        raise ValueError(f"Modelo no alcanza precisión mínima: {accuracy}")
    
    return {'validation_passed': True}

ml_dag = DAG(
    'ml_productos_pipeline',
    default_args=default_args,
    description='Pipeline de ML para clasificación de productos',
    schedule_interval='0 4 * * 1',  # Lunes 4 AM
    catchup=False
)

# Tasks del pipeline ML
data_sensor = FileSensor(
    task_id='wait_for_training_data',
    filepath='/data/productos_training.csv',
    fs_conn_id='fs_default',
    poke_interval=300,
    timeout=3600,
    dag=ml_dag
)

prepare_data = PythonOperator(
    task_id='prepare_training_data',
    python_callable=prepare_training_data,
    dag=ml_dag
)

train_model_task = DockerOperator(
    task_id='train_model',
    image='python:3.9-slim',
    command='python /scripts/train_model.py',
    volumes=['/opt/airflow/models:/models', '/opt/airflow/data:/data'],
    dag=ml_dag
)

validate_model_task = PythonOperator(
    task_id='validate_model',
    python_callable=validate_model,
    dag=ml_dag
)

deploy_model = BashOperator(
    task_id='deploy_model',
    bash_command="""
    curl -X POST http://mlflow:5000/api/2.0/mlflow/model-versions/transition-stage \
    -H 'Content-Type: application/json' \
    -d '{"name": "productos-classifier", "version": "{{ ds }}", "stage": "Production"}'
    """,
    dag=ml_dag
)

# Dependencias
data_sensor >> prepare_data >> train_model_task >> validate_model_task >> deploy_model
```

### 3. **DAG con Sensores y Triggers Externos**

#### Reaccionar a cambios en sistemas externos
```python
# dags/event_driven_dag.py
from airflow.sensors.s3_key_sensor import S3KeySensor
from airflow.sensors.sql_sensor import SqlSensor
from airflow.providers.http.sensors.http import HttpSensor

event_dag = DAG(
    'event_driven_processing',
    default_args=default_args,
    description='Procesamiento basado en eventos',
    schedule_interval=None,  # Activado por triggers
    catchup=False
)

# Sensor para nuevos archivos en S3/MinIO
file_sensor = S3KeySensor(
    task_id='wait_for_new_file',
    bucket_name='data-lake',
    bucket_key='incoming/productos/{{ ds }}/data.csv',
    aws_conn_id='minio_default',
    poke_interval=60,
    timeout=3600,
    dag=event_dag
)

# Sensor para cambios en base de datos
db_sensor = SqlSensor(
    task_id='wait_for_db_update',
    conn_id='postgres_default',
    sql="""
    SELECT COUNT(*) FROM productos 
    WHERE updated_at > '{{ macros.datetime.utcnow() - macros.timedelta(minutes=30) }}'
    """,
    poke_interval=300,
    dag=event_dag
)

# Sensor para API externa
api_sensor = HttpSensor(
    task_id='wait_for_api_data',
    http_conn_id='external_api',
    endpoint='api/data/status',
    request_params={'date': '{{ ds }}'},
    poke_interval=120,
    dag=event_dag
)

# Proceso cuando todos los sensores se activan
process_all_data = PythonOperator(
    task_id='process_combined_data',
    python_callable=lambda: print("Procesando datos de múltiples fuentes"),
    dag=event_dag
)

[file_sensor, db_sensor, api_sensor] >> process_all_data
```

### 4. **Configuración de Connections y Variables**

#### Setup via Airflow CLI
```bash
# Crear conexiones
docker exec airflow-webserver airflow connections add \
  'postgres_default' \
  --conn-type 'postgres' \
  --conn-host 'postgres' \
  --conn-login 'admin' \
  --conn-password 'admin123' \
  --conn-schema 'productos_db' \
  --conn-port 5432

docker exec airflow-webserver airflow connections add \
  'mongo_default' \
  --conn-type 'mongo' \
  --conn-host 'mongo' \
  --conn-port 27017 \
  --conn-schema 'productos_db'

# Crear variables
docker exec airflow-webserver airflow variables set \
  'data_quality_threshold' '0.95'

docker exec airflow-webserver airflow variables set \
  'notification_email' 'data-team@empresa.com'

docker exec airflow-webserver airflow variables set \
  'ml_model_version' '1.0.0'
```

#### Setup via Web UI o API
```bash
# Via REST API
curl -X POST "http://localhost:8086/api/v1/connections" \
  -H "Content-Type: application/json" \
  -u "admin:admin123" \
  -d '{
    "connection_id": "kafka_default",
    "conn_type": "kafka",
    "host": "kafka",
    "port": 29092,
    "extra": "{\"bootstrap.servers\": \"kafka:29092\"}"
  }'

# Listar DAGs
curl "http://localhost:8086/api/v1/dags" \
  -u "admin:admin123" | jq '.dags[].dag_id'

# Trigger DAG manualmente
curl -X POST "http://localhost:8086/api/v1/dags/productos_etl_daily/dagRuns" \
  -H "Content-Type: application/json" \
  -u "admin:admin123" \
  -d '{
    "conf": {"manual_trigger": true},
    "execution_date": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"
  }'
```

### 5. **Monitoring y Alertas**

#### Configurar alertas por email y Slack
```python
# airflow_settings.py - Custom alerts
def task_fail_slack_alert(context):
    """Enviar alerta a Slack cuando falla una tarea"""
    import requests
    
    slack_webhook = Variable.get('slack_webhook_url')
    task_instance = context.get('task_instance')
    dag_id = task_instance.dag_id
    task_id = task_instance.task_id
    execution_date = context.get('execution_date')
    log_url = task_instance.log_url
    
    message = {
        "text": f"❌ Airflow Task Failed",
        "attachments": [
            {
                "color": "danger",
                "fields": [
                    {"title": "DAG", "value": dag_id, "short": True},
                    {"title": "Task", "value": task_id, "short": True},
                    {"title": "Execution Date", "value": str(execution_date), "short": True},
                    {"title": "Log URL", "value": log_url, "short": True}
                ]
            }
        ]
    }
    
    requests.post(slack_webhook, json=message)

# Usar en DAGs
default_args = {
    'on_failure_callback': task_fail_slack_alert,
    'email_on_failure': True,
    'email': ['data-team@empresa.com']
}
```

## 🔧 Comandos Útiles

### Gestión del Stack
```bash
# Iniciar workflow stack
./manage-stack.sh start workflow

# Ver logs de Airflow
docker logs airflow-webserver -f
docker logs airflow-scheduler -f

# Reiniciar scheduler
docker restart airflow-scheduler

# Acceder al CLI de Airflow
docker exec -it airflow-webserver airflow --help
```

### Administración de DAGs
```bash
# Listar DAGs
docker exec airflow-webserver airflow dags list

# Estado de DAG específico
docker exec airflow-webserver airflow dags state productos_etl_daily 2024-01-15

# Trigger manual de DAG
docker exec airflow-webserver airflow dags trigger productos_etl_daily

# Pausar/despausar DAG
docker exec airflow-webserver airflow dags pause productos_etl_daily
docker exec airflow-webserver airflow dags unpause productos_etl_daily

# Ver próximas ejecuciones
docker exec airflow-webserver airflow dags next-execution productos_etl_daily
```

### Task Management
```bash
# Listar tasks de un DAG
docker exec airflow-webserver airflow tasks list productos_etl_daily

# Estado de task específica
docker exec airflow-webserver airflow tasks state productos_etl_daily extract_productos 2024-01-15

# Ejecutar task individualmente
docker exec airflow-webserver airflow tasks run productos_etl_daily extract_productos 2024-01-15

# Ver logs de task
docker exec airflow-webserver airflow tasks logs productos_etl_daily extract_productos 2024-01-15

# Clear task (para re-ejecutar)
docker exec airflow-webserver airflow tasks clear productos_etl_daily --start-date 2024-01-15 --end-date 2024-01-15
```

### Database Operations
```bash
# Upgrade database schema
docker exec airflow-webserver airflow db upgrade

# Crear usuario admin adicional
docker exec airflow-webserver airflow users create \
  --username dataeng \
  --firstname Data \
  --lastname Engineer \
  --role Admin \
  --email dataeng@empresa.com \
  --password dataeng123

# Backup de metadatos
docker exec airflow-postgres pg_dump -U airflow airflow > airflow-backup.sql

# Ver estadísticas de la base de datos
docker exec airflow-postgres psql -U airflow -d airflow \
  -c "SELECT dag_id, COUNT(*) as runs FROM dag_run GROUP BY dag_id;"
```

## 🔍 Troubleshooting

### Problemas Comunes

1. **Scheduler no ejecuta DAGs**
   ```bash
   # Verificar que el scheduler esté corriendo
   docker logs airflow-scheduler --tail 50
   
   # Ver estado del scheduler
   curl "http://localhost:8086/api/v1/health" -u "admin:admin123"
   
   # Restart scheduler
   docker restart airflow-scheduler
   
   # Verificar DAGs parseados
   docker exec airflow-webserver airflow dags list-import-errors
   ```

2. **Tasks fallan constantemente**
   ```bash
   # Ver logs detallados de task
   docker exec airflow-webserver airflow tasks logs productos_etl_daily extract_productos 2024-01-15 -v
   
   # Verificar conexiones
   docker exec airflow-webserver airflow connections test postgres_default
   
   # Verificar variables
   docker exec airflow-webserver airflow variables list
   ```

3. **Performance issues**
   ```bash
   # Ver estadísticas del scheduler
   curl "http://localhost:8086/api/v1/pools" -u "admin:admin123"
   
   # Verificar workers de Celery
   docker exec airflow-webserver celery -A airflow.executors.celery_executor.celery_app inspect active
   
   # Ver uso de memoria
   docker stats airflow-webserver airflow-scheduler
   ```

4. **Web UI no accesible**
   ```bash
   # Verificar que webserver esté corriendo
   docker logs airflow-webserver --tail 100
   
   # Test de conectividad
   curl -I http://localhost:8086/health
   
   # Verificar configuración de auth
   docker exec airflow-webserver airflow config get-value webserver authenticate
   ```

## 📊 Monitoreo y Métricas

### Health Checks y APIs
```bash
# Health check general
curl "http://localhost:8086/api/v1/health" -u "admin:admin123"

# Estado del scheduler
curl "http://localhost:8086/api/v1/dags" -u "admin:admin123" | jq '.total_entries'

# Pool de workers
curl "http://localhost:8086/api/v1/pools" -u "admin:admin123"

# DAG runs recientes
curl "http://localhost:8086/api/v1/dags/productos_etl_daily/dagRuns?limit=10" -u "admin:admin123"
```

### Métricas Custom
```python
# En DAGs - tracking custom metrics
from airflow.providers.postgres.hooks.postgres import PostgresHook
from airflow.models import Variable

def track_data_quality(**context):
    """Track data quality metrics"""
    pg_hook = PostgresHook(postgres_conn_id='postgres_default')
    
    sql = """
    SELECT 
        COUNT(*) as total_records,
        COUNT(CASE WHEN precio > 0 THEN 1 END) as valid_prices,
        AVG(precio) as avg_price
    FROM productos
    """
    
    result = pg_hook.get_first(sql)
    
    # Enviar métricas a sistema de monitoreo
    metrics = {
        'total_records': result[0],
        'valid_prices': result[1],
        'data_quality_score': result[1] / result[0] if result[0] > 0 else 0
    }
    
    # Log metrics