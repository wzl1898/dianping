#!/bin/bash
# MySQL setup script
set -e
MYSQL_HOME=/home/zlwu/miniconda3/envs/pdformer
export PATH="$MYSQL_HOME/bin:$PATH"

# Setup directories
mkdir -p /home/zlwu/mysql-data /home/zlwu/mysql-logs

# Create config
cat > /tmp/my.cnf << 'CFG'
[mysqld]
basedir=/home/zlwu/miniconda3/envs/pdformer
datadir=/home/zlwu/mysql-data
socket=/home/zlwu/mysql-data/mysql.sock
port=3307
log-error=/home/zlwu/mysql-logs/error.log
pid-file=/home/zlwu/mysql-data/mysql.pid
user=zlwu
skip-grant-tables
CFG

# Initialize if needed
if [ ! -f /home/zlwu/mysql-data/ibdata1 ]; then
    echo "Initializing MySQL data directory..."
    mysqld --defaults-file=/tmp/my.cnf --initialize-insecure
fi

# Start MySQL
echo "Starting MySQL on port 3307..."
mysqld --defaults-file=/tmp/my.cnf --daemonize --skip-grant-tables

sleep 2

# Create database and user
mysql -S /home/zlwu/mysql-data/mysql.sock -e "FLUSH PRIVILEGES;"
mysql -S /home/zlwu/mysql-data/mysql.sock -e "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';"
mysql -S /home/zlwu/mysql-data/mysql.sock -e "CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED BY 'root';"
mysql -S /home/zlwu/mysql-data/mysql.sock -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;"
mysql -S /home/zlwu/mysql-data/mysql.sock -e "FLUSH PRIVILEGES;"

# Create database
mysql -h 127.0.0.1 -P 3307 -u root -proot -e "CREATE DATABASE IF NOT EXISTS dianping DEFAULT CHARACTER SET utf8mb4;"

echo "MySQL ready on port 3307"
