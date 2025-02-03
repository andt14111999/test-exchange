# frozen_string_literal: true

class ApplicationMailer < ActionMailer::Base
  default from: 'dev@snowfoxglobal.org'
  layout 'mailer'
end
