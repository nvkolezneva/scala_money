package misis.model

case class AccountToAck(sourceId: Int, destinationId: Int, value: Int) extends Command
