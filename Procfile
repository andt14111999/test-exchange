web: bundle exec puma -C config/puma.rb
release: bundle exec rails db:migrate
worker: bundle exec sidekiq -e production -C config/sidekiq.yml
clockwork: bundle exec clockwork config/clockwork.rb
