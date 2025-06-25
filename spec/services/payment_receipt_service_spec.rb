# frozen_string_literal: true

require 'rails_helper'

RSpec.describe PaymentReceiptService, type: :service do
  describe '#process_payment_receipt' do
    let(:trade) { create(:trade) }
    let(:service) { described_class.new(trade) }
    let(:receipt_details) { { 'transaction_id' => 'TX12345', 'amount' => '500' } }

    # Mock ENV variables to avoid Kafka configuration issues
    before do
      allow(ENV).to receive(:fetch).and_call_original
      allow(ENV).to receive(:fetch).with('KAFKA_SSL_ENABLED', 'false').and_return('false')
      allow(ENV).to receive(:fetch).with('EXCHANGE_BACKEND_DOMAIN', 'snow.exchange').and_return('snow.exchange')
    end

    context 'with valid file' do
      let(:file) do
        file = fixture_file_upload(
          Rails.root.join('spec', 'fixtures', 'files', 'test.jpg'),
          'image/jpeg'
        )
        {
          tempfile: file,
          filename: 'test.jpg',
          type: 'image/jpeg'
        }
      end

      it 'processes payment receipt successfully' do
        result = service.process_payment_receipt(receipt_details, file)

        expect(result).to be true
        expect(trade.payment_receipt_details).to include('transaction_id' => 'TX12345')
        expect(trade.payment_receipt_details).to include('amount' => '500')
        expect(trade.payment_receipt_details).to have_key('file_url')
        expect(trade.has_payment_proof).to be true
        expect(trade.payment_receipt_file).to be_attached
      end

      it 'generates correct URL in development environment' do
        service.process_payment_receipt(receipt_details, file)

        expect(trade.payment_receipt_details['file_url']).to include('/rails/active_storage/blobs/')
        expect(trade.payment_receipt_details['file_url']).to include('example.com')
      end

      it 'generates correct URL in production environment' do
        allow(Rails.env).to receive(:production?).and_return(true)

        service.process_payment_receipt(receipt_details, file)

        expect(trade.payment_receipt_details['file_url']).to include('/rails/active_storage/blobs/')
        expect(trade.payment_receipt_details['file_url']).to include('snow.exchange')
      end

      it 'handles missing host configuration gracefully' do
        # Test by making rails_blob_url fail
        allow(Rails.application.routes.url_helpers).to receive(:rails_blob_url)
          .and_raise(ArgumentError, 'Missing host configuration')
        allow(Rails.logger).to receive(:warn)

        service.process_payment_receipt(receipt_details, file)

        expect(Rails.logger).to have_received(:warn).with(/Could not generate file URL/)
        expect(trade.payment_receipt_details['file_url']).to be_nil
      end

      it 'sanitizes filename correctly' do
        file_with_special_chars = file.merge(filename: 'test@#$%^&*.jpg')
        service.process_payment_receipt(receipt_details, file_with_special_chars)

        expect(trade.payment_receipt_file.filename.to_s).to eq('test_______.jpg')
      end
    end

    context 'without file' do
      it 'processes payment receipt successfully' do
        result = service.process_payment_receipt(receipt_details)

        expect(result).to be true
        expect(trade.payment_receipt_details).to eq(receipt_details)
        expect(trade.payment_receipt_details).not_to have_key('file_url')
        expect(trade.has_payment_proof).to be true
        expect(trade.payment_receipt_file).not_to be_attached
      end
    end

    context 'with invalid file size' do
      let(:large_file) do
        file = fixture_file_upload(
          Rails.root.join('spec', 'fixtures', 'files', 'test.jpg'),
          'image/jpeg'
        )
        {
          tempfile: file,
          filename: 'test.jpg',
          type: 'image/jpeg'
        }.tap do |f|
          allow(f[:tempfile]).to receive(:size).and_return(20.megabytes)
        end
      end

      it 'returns false for oversized file' do
        expect {
          service.process_payment_receipt(receipt_details, large_file)
        }.to raise_error(RuntimeError, /File size must be less than 10MB/)
      end
    end

    context 'with invalid file type' do
      let(:invalid_file) do
        file = fixture_file_upload(
          Rails.root.join('spec', 'fixtures', 'files', 'test.jpg'),
          'text/plain'
        )
        {
          tempfile: file,
          filename: 'test.jpg',
          type: 'text/plain'
        }
      end

      it 'returns false for invalid file type' do
        expect {
          service.process_payment_receipt(receipt_details, invalid_file)
        }.to raise_error(RuntimeError, /File type not allowed/)
      end
    end

    context 'with attachment failures' do
      let(:file) do
        file = fixture_file_upload(
          Rails.root.join('spec', 'fixtures', 'files', 'test.jpg'),
          'image/jpeg'
        )
        {
          tempfile: file,
          filename: 'test.jpg',
          type: 'image/jpeg'
        }
      end

      it 'handles attachment failures' do
        allow(trade.payment_receipt_file).to receive(:attach).and_raise(ActiveStorage::Error)

        expect {
          service.process_payment_receipt(receipt_details, file)
        }.to raise_error(ActiveStorage::Error)
      end

      it 'handles trade update failures' do
        allow(trade).to receive(:update!).and_raise(ActiveRecord::RecordInvalid)

        expect {
          service.process_payment_receipt(receipt_details, file)
        }.to raise_error(ActiveRecord::RecordInvalid)
      end
    end

    context 'with various file types' do
      %w[image/jpeg image/png image/gif application/pdf].each do |mime_type|
        it "accepts #{mime_type} files" do
          file = {
            tempfile: fixture_file_upload(
              Rails.root.join('spec', 'fixtures', 'files', 'test.jpg'),
              mime_type
            ),
            filename: 'test.jpg',
            type: mime_type
          }

          expect {
            service.process_payment_receipt(receipt_details, file)
          }.not_to raise_error
        end
      end
    end
  end
end
