docker run --name jsql-mysql  \
--publish 3306:3306 \
-e MYSQL_ROOT_PASSWORD=my-secret-pw \
-d mysql

docker run --name jsql-mysql-5.5.40 \
--publish 3307:3306 \
-e MYSQL_ROOT_PASSWORD=my-secret-pw \
-d mysql:5.5.40

docker run --name jsql-postgres \
--publish 5432:5432 \
-e POSTGRES_PASSWORD=my-secret-pw \
-d postgres \
-c 'shared_buffers=256MB'\
-c 'max_connections=1000'

docker run --name jsql-sqlserver \
--publish 1434:1434 \
--publish 1433:1433 \
-e "ACCEPT_EULA=Y" \
-e "SA_PASSWORD=yourStrong(!)Password"\
-d sqlserver 

docker images && docker ps && pwd