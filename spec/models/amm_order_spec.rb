# frozen_string_literal: true

require 'rails_helper'

describe AmmOrder, type: :model do
  describe 'validations' do
    before do
      allow_any_instance_of(described_class).to receive(:user_has_sufficient_balance).and_return(true)
      allow_any_instance_of(described_class).to receive(:process_order).and_return(true)
    end

    it 'has a valid factory' do
      expect(build(:amm_order)).to be_valid
    end

    it 'requires identifier' do
      order = build(:amm_order, identifier: nil)
      expect(order).to be_invalid
      expect(order.errors.full_messages).to include("Identifier can't be blank")
    end

    it 'requires a unique identifier' do
      create(:amm_order, identifier: 'test_identifier')
      order = build(:amm_order, identifier: 'test_identifier')
      expect(order).to be_invalid
      expect(order.errors.full_messages).to include('Identifier has already been taken')
    end

    it 'requires amount_specified to not be zero' do
      order = build(:amm_order, amount_specified: 0)
      expect(order).to be_invalid
      expect(order.errors.full_messages).to include("Amount specified must be other than 0")
    end

    it 'requires amount_estimated to be non-negative' do
      order = build(:amm_order, amount_estimated: -1)
      expect(order).to be_invalid
      expect(order.errors.full_messages).to include("Amount estimated must be greater than or equal to 0")
    end

    it 'requires amount_actual to be non-negative' do
      order = build(:amm_order, amount_actual: -1)
      expect(order).to be_invalid
      expect(order.errors.full_messages).to include("Amount actual must be greater than or equal to 0")
    end

    it 'requires amount_received to be non-negative' do
      order = build(:amm_order, amount_received: -1)
      expect(order).to be_invalid
      expect(order.errors.full_messages).to include("Amount received must be greater than or equal to 0")
    end

    it 'requires slippage to be non-negative' do
      order = build(:amm_order, slippage: -0.01)
      expect(order).to be_invalid
      expect(order.errors.full_messages).to include("Slippage must be greater than or equal to 0")
    end

    it 'validates user has sufficient balance' do
      amm_pool = create(:amm_pool, pair: 'BTC/USDT', token0: 'BTC', token1: 'USDT')
      user = create(:user)
      order = build(
        :amm_order,
        user: user,
        amm_pool: amm_pool,
        zero_for_one: true,
        amount_specified: 100,
        skip_balance_validation: false
      )

      allow(order).to receive(:user_has_sufficient_balance).and_call_original

      expect(order).to be_invalid
      expect(order.errors.full_messages).to include(/Không đủ số dư/)
    end

    it 'skips balance validation when skip_balance_validation is true' do
      amm_pool = create(:amm_pool, pair: 'BTC/USDT', token0: 'BTC', token1: 'USDT')
      user = create(:user)
      order = build(:amm_order, user: user, amm_pool: amm_pool, skip_balance_validation: true, amount_specified: 100, zero_for_one: true)

      allow(user).to receive(:main_account).with('BTC').and_return(nil)

      expect(order).to be_valid
    end
  end

  describe 'associations' do
    it 'belongs to user' do
      expect(described_class.reflect_on_association(:user).macro).to eq(:belongs_to)
    end

    it 'belongs to amm_pool' do
      expect(described_class.reflect_on_association(:amm_pool).macro).to eq(:belongs_to)
    end
  end

  describe 'after_create callback' do
    it 'calls process_order after create' do
      order = build(:amm_order)
      allow(order).to receive(:user_has_sufficient_balance).and_return(true)
      expect(order).to receive(:process_order)
      order.save!
    end
  end

  describe 'process_order method' do
    it 'processes the order and sends event if processing' do
      expect_any_instance_of(described_class).to receive(:send_event_create_amm_order)

      create(:amm_order)
    end
  end

  describe 'user_has_sufficient_balance method' do
    before do
      allow_any_instance_of(described_class).to receive(:process_order).and_return(true)
    end

    context 'khi amount_specified > 0' do
      it 'kiểm tra token0 khi zero_for_one là true' do
        amm_pool = create(:amm_pool)
        user = create(:user)

        allow(amm_pool).to receive(:token0).and_return('BTC')
        account = double('Account', balance: 50)
        order = build(:amm_order,
                     user: user,
                     amm_pool: amm_pool,
                     amount_specified: 100,
                     zero_for_one: true,
                     skip_balance_validation: false)

        allow(user).to receive(:main_account).with('BTC').and_return(account)
        order.send(:user_has_sufficient_balance)

        expect(order.errors.full_messages).to include("Không đủ số dư token0 để thực hiện giao dịch")
      end

      it 'kiểm tra token1 khi zero_for_one là false' do
        amm_pool = create(:amm_pool)
        user = create(:user)

        # Tạo tài khoản với số dư không đủ
        allow(amm_pool).to receive(:token1).and_return('USDT')
        account = double('Account', balance: 50)

        order = build(:amm_order,
                     user: user,
                     amm_pool: amm_pool,
                     amount_specified: 100,
                     zero_for_one: false,
                     skip_balance_validation: false)

        # Giả lập token và tài khoản
        allow(user).to receive(:main_account).with('USDT').and_return(account)

        # Gọi phương thức user_has_sufficient_balance trực tiếp
        order.send(:user_has_sufficient_balance)

        expect(order.errors.full_messages).to include("Không đủ số dư token1 để thực hiện giao dịch")
      end

      it 'không có lỗi khi có đủ số dư' do
        amm_pool = create(:amm_pool)
        user = create(:user)

        # Tạo tài khoản với số dư đủ
        allow(amm_pool).to receive(:token0).and_return('BTC')
        account = double('Account', balance: 200)

        order = build(:amm_order,
                     user: user,
                     amm_pool: amm_pool,
                     amount_specified: 100,
                     zero_for_one: true,
                     skip_balance_validation: false)

        allow(user).to receive(:main_account).with('BTC').and_return(account)

        order.send(:user_has_sufficient_balance)

        expect(order.errors).to be_empty
      end
    end

    context 'khi amount_specified < 0' do
      it 'kiểm tra token1 khi zero_for_one là true với amount_estimated' do
        amm_pool = create(:amm_pool)
        user = create(:user)

        # Tạo tài khoản với số dư không đủ
        allow(amm_pool).to receive(:token1).and_return('USDT')
        account = double('Account', balance: 50)

        order = build(:amm_order,
                     user: user,
                     amm_pool: amm_pool,
                     amount_specified: -100,
                     amount_estimated: 200,
                     zero_for_one: true,
                     skip_balance_validation: false)

        # Giả lập token và tài khoản
        allow(user).to receive(:main_account).with('USDT').and_return(account)

        # Gọi phương thức user_has_sufficient_balance trực tiếp
        order.send(:user_has_sufficient_balance)

        expect(order.errors.full_messages).to include("Không đủ số dư token1 để thực hiện giao dịch")
      end

      it 'kiểm tra token0 khi zero_for_one là false với amount_estimated' do
        amm_pool = create(:amm_pool)
        user = create(:user)

        # Tạo tài khoản với số dư không đủ
        allow(amm_pool).to receive(:token0).and_return('BTC')
        account = double('Account', balance: 50)

        order = build(:amm_order,
                     user: user,
                     amm_pool: amm_pool,
                     amount_specified: -100,
                     amount_estimated: 200,
                     zero_for_one: false,
                     skip_balance_validation: false)

        # Giả lập token và tài khoản
        allow(user).to receive(:main_account).with('BTC').and_return(account)

        # Gọi phương thức user_has_sufficient_balance trực tiếp
        order.send(:user_has_sufficient_balance)

        expect(order.errors.full_messages).to include("Không đủ số dư token0 để thực hiện giao dịch")
      end

      it 'không có lỗi khi có đủ số dư' do
        amm_pool = create(:amm_pool)
        user = create(:user)

        # Tạo tài khoản với số dư đủ
        allow(amm_pool).to receive(:token1).and_return('USDT')
        account = double('Account', balance: 500)

        order = build(:amm_order,
                     user: user,
                     amm_pool: amm_pool,
                     amount_specified: -100,
                     amount_estimated: 200,
                     zero_for_one: true,
                     skip_balance_validation: false)

        # Giả lập token và tài khoản
        allow(user).to receive(:main_account).with('USDT').and_return(account)

        # Gọi phương thức user_has_sufficient_balance trực tiếp
        order.send(:user_has_sufficient_balance)

        expect(order.errors).to be_empty
      end
    end

    context 'khi không tìm thấy tài khoản' do
      it 'thêm lỗi khi tài khoản không tồn tại' do
        amm_pool = create(:amm_pool)
        user = create(:user)

        # Không có tài khoản
        allow(amm_pool).to receive(:token0).and_return('BTC')

        order = build(:amm_order,
                     user: user,
                     amm_pool: amm_pool,
                     amount_specified: 100,
                     zero_for_one: true,
                     skip_balance_validation: false)

        allow(user).to receive(:main_account).with('BTC').and_return(nil)

        order.send(:user_has_sufficient_balance)

        expect(order.errors.full_messages).to include("Không đủ số dư token0 để thực hiện giao dịch")
      end
    end
  end

  describe 'send_event_create_amm_order method' do
    it 'sends event with correct params to Kafka service' do
      allow_any_instance_of(described_class).to receive(:process_order).and_return(true)

      amm_pool = create(:amm_pool, pair: 'BTC/USDT')
      user = create(:user)

      order = create(:amm_order,
                    user: user,
                    amm_pool: amm_pool,
                    identifier: 'test_id',
                    zero_for_one: true,
                    amount_specified: 100,
                    slippage: 0.01,
                    status: 'processing',
                    id: 123)

      allow(order).to receive_messages(account_key0: 'acc_key0', account_key1: 'acc_key1')

      allow(SecureRandom).to receive(:uuid).and_return('test-uuid')

      service = double('KafkaService::Services::AmmOrder::AmmOrderService')
      allow(KafkaService::Services::AmmOrder::AmmOrderService).to receive(:new).and_return(service)

      expected_payload = {
        eventId: "amm-order-test-uuid",
        operationType: 'amm_order_swap',
        actionType: "AmmOrder",
        status: "processing",
        actionId: 123,
        identifier: "test_id",
        poolPair: "BTC/USDT",
        ownerAccountKey0: "acc_key0",
        ownerAccountKey1: "acc_key1",
        zeroForOne: true,
        amountSpecified: 100,
        slippage: 0.01
      }

      expect(service).to receive(:create).with(identifier: 'test_id', payload: expected_payload)

      order.send(:send_event_create_amm_order)
    end

    it 'ghi log lỗi và cập nhật trạng thái khi có ngoại lệ StandardError' do
      allow_any_instance_of(described_class).to receive(:process_order).and_return(true)

      amm_pool = create(:amm_pool, pair: 'BTC/USDT')
      user = create(:user)

      order = create(:amm_order,
                    user: user,
                    amm_pool: amm_pool,
                    status: 'processing')

      service = double('KafkaService::Services::AmmOrder::AmmOrderService')
      allow(KafkaService::Services::AmmOrder::AmmOrderService).to receive(:new).and_return(service)
      error_message = "Kafka connection failure"
      allow(service).to receive(:create).and_raise(StandardError.new(error_message))

      logger_double = double('Logger')
      allow(Rails).to receive(:logger).and_return(logger_double)

      expect(logger_double).to receive(:error).with(/Failed to notify exchange engine about AmmOrder creation: #{error_message}/)

      expect(order).to receive(:fail).with(error_message)

      order.send(:send_event_create_amm_order)
    end
  end

  describe 'delegations' do
    it 'delegates pair to amm_pool' do
      amm_pool = create(:amm_pool, pair: 'BTC/USDT')
      order = create(:amm_order, amm_pool: amm_pool)
      expect(order.pair).to eq('BTC/USDT')
    end

    it 'delegates token0 to amm_pool' do
      amm_pool = create(:amm_pool, token0: 'BTC')
      order = create(:amm_order, amm_pool: amm_pool)
      expect(order.token0).to eq('BTC')
    end

    it 'delegates token1 to amm_pool' do
      amm_pool = create(:amm_pool, token1: 'USDT')
      order = create(:amm_order, amm_pool: amm_pool)
      expect(order.token1).to eq('USDT')
    end
  end

  describe 'instance methods' do
    describe '#account_key0' do
      it 'returns the account key for token0' do
        amm_pool = create(:amm_pool, token0: 'BTC')
        user = create(:user)
        order = create(:amm_order, user: user, amm_pool: amm_pool)

        account = double('Account', account_key: 'key0')
        allow(user).to receive(:main_account).with('BTC').and_return(account)

        expect(order.account_key0).to eq('key0')
      end
    end

    describe '#account_key1' do
      it 'returns the account key for token1' do
        amm_pool = create(:amm_pool, token1: 'USDT')
        user = create(:user)
        order = create(:amm_order, user: user, amm_pool: amm_pool)

        # Giả lập account
        account = double('Account', account_key: 'key1')
        allow(user).to receive(:main_account).with('USDT').and_return(account)

        expect(order.account_key1).to eq('key1')
      end
    end

    describe '#generate_identifier' do
      it 'generates an identifier for the order' do
        user = create(:user, id: 123)
        amm_pool = create(:amm_pool, pair: 'BTC/USDT')

        order = build(:amm_order, user: user, amm_pool: amm_pool, identifier: nil)
        order.generate_identifier

        expect(order.identifier).to match(/^amm_order_123_btc\/usdt_\d+$/)
      end
    end
  end

  describe 'class methods' do
    describe '.generate_identifier' do
      it 'generates an identifier with the given parameters' do
        user_id = 123
        pool_pair = 'BTC/USDT'
        timestamp = 1620000000

        identifier = described_class.generate_identifier(user_id, pool_pair, timestamp)
        expect(identifier).to eq("amm_order_123_btc/usdt_1620000000")
      end

      it 'uses the current timestamp if not provided' do
        user_id = 123
        pool_pair = 'BTC/USDT'
        allow(Time.zone).to receive(:now).and_return(Time.at(1620000000))

        identifier = described_class.generate_identifier(user_id, pool_pair)
        expect(identifier).to eq("amm_order_123_btc/usdt_1620000000")
      end
    end
  end

  describe 'AASM states and transitions' do
    before do
      allow_any_instance_of(described_class).to receive(:process_order).and_return(true)
    end

    it 'has initial state pending' do
      order = create(:amm_order, status: 'pending')
      expect(order.status).to eq('pending')
      expect(order).to be_pending
    end

    it 'transitions from pending to processing on process event' do
      order = create(:amm_order, status: 'pending')

      expect {
        order.process!
      }.to change { order.status }.from('pending').to('processing')

      expect(order).to be_processing
    end

    it 'transitions from processing to success on succeed event' do
      order = create(:amm_order, status: 'processing')

      expect {
        order.succeed!
      }.to change { order.status }.from('processing').to('success')

      expect(order).to be_success
    end

    it 'transitions from pending to error on fail event' do
      order = create(:amm_order, status: 'pending')

      expect {
        order.fail!('Error message')
      }.to change { order.status }.from('pending').to('error')

      expect(order).to be_error
      expect(order.error_message).to eq('Error message')
    end

    it 'transitions from processing to error on fail event' do
      order = create(:amm_order, status: 'processing')

      expect {
        order.fail!('Error message')
      }.to change { order.status }.from('processing').to('error')

      expect(order).to be_error
      expect(order.error_message).to eq('Error message')
    end
  end

  describe 'after_update callback' do
    before do
      allow_any_instance_of(described_class).to receive(:process_order).and_return(true)
    end

    it 'calls broadcast_amm_order_update when status changes' do
      order = create(:amm_order, status: 'pending')

      expect(order).to receive(:broadcast_amm_order_update)

      order.update(status: 'processing')
    end

    it 'does not call broadcast_amm_order_update when status does not change' do
      order = create(:amm_order, status: 'pending')

      expect(order).not_to receive(:broadcast_amm_order_update)

      order.update(amount_specified: 200)
    end
  end

  describe '#broadcast_amm_order_update' do
    let(:user) { create(:user) }
    let(:order) { create(:amm_order, user: user) }

    before do
      allow_any_instance_of(described_class).to receive(:process_order).and_return(true)
    end

    it 'calls AmmOrderBroadcastService with user' do
      expect(AmmOrderBroadcastService).to receive(:call).with(user)

      order.send(:broadcast_amm_order_update)
    end

    it 'logs error when broadcast fails' do
      error = StandardError.new('Broadcast failed')
      allow(AmmOrderBroadcastService).to receive(:call).and_raise(error)

      expect(Rails.logger).to receive(:error).with('Failed to broadcast AMM order update: Broadcast failed')

      order.send(:broadcast_amm_order_update)
    end
  end
end
