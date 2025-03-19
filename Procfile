web: bundle exec puma -C config/puma.rb
release: bundle exec rails db:migrate
worker: bundle exec sidekiq -e production -C config/sidekiq.yml
clockwork: bundle exec clockwork config/clockwork.rb
kafka_consumer: bundle exec ruby lib/daemons/kafka_consumer_daemon.rb
