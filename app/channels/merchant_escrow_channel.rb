# frozen_string_literal: true

class MerchantEscrowChannel < ApplicationCable::Channel
  def self.broadcast_to_merchant_escrow(merchant_escrow, data)
    broadcast_to(merchant_escrow, data)
  end

  def subscribed
    merchant_escrow = MerchantEscrow.where(id: params[:escrow_id])
                                  .where(user_id: current_user.id)
                                  .first

    if merchant_escrow
      stream_for merchant_escrow
    else
      reject
    end
  end

  def unsubscribed
    stop_all_streams
  end

  def keepalive(_data)
    transmit({ status: 'success', message: 'keepalive_ack', timestamp: Time.current.to_i })
  end
end
