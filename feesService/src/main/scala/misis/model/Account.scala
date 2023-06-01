package misis.model

case class AccountFreeLimit(accountId: Int, freeLimit: Int) {
    def updateFreeLimit(value: Int) = this.copy(freeLimit = freeLimit - value)
}

trait Command
case class AccountUpdate(
    accountId: Int,
    value: Int,
    feeValue: Int = 0,
    nextAccountId: Option[Int] = None
) extends Command

trait Event
case class AccountUpdated(
    accountId: Int,
    value: Int,
    feeValue: Int = 0,
    nextAccountId: Option[Int] = None,
    previousAccountId: Option[Int] = None,
    categoryId: Option[Int] = None
) extends Event
