package misis.repository

import misis.kafka.TopicName
import misis.model._
import io.circe.generic.auto._
import scala.concurrent.Future
import scala.collection.mutable

class Repository() {
    private val accountsCashbacks = mutable.Map.empty[Int, AccountCashback]
    private val categoryIdCashbackPercent = mutable.Map.empty[Int, Int]
    // Hard coded values
    categoryIdCashbackPercent += (1 -> 10)
    categoryIdCashbackPercent += (2 -> 20)
    categoryIdCashbackPercent += (3 -> 30)

    def accountCashbackExists(accountId: Int): Boolean = {
        accountsCashbacks.contains(accountId)
    }

    def createAccountCashback(accountId: Int, cashback: Int = 0): Future[AccountCashback] = {
        val accountCashback = AccountCashback(accountId, cashback)
        accountsCashbacks += (accountId -> accountCashback)
        Future.successful(accountCashback)
    }

    def updateCashback(accountId: Int, value: Int): Future[Unit] = {
        accountsCashbacks.get(accountId) match {
            case Some(accountsCashback) =>
                val updatedCashback = accountsCashback.updateCashback(value)
                accountsCashbacks += (accountId -> updatedCashback)
                Future.successful(())
            case None =>
                Future.failed(new IllegalArgumentException(s"Limit with account $accountId does not exist"))
        }
    }

    def resetCashback(accountId: Int): Future[Unit] = {
        accountsCashbacks.get(accountId) match {
            case Some(accountsCashback) =>
                val updatedCashback = accountsCashback.resetCashback()
                accountsCashbacks += (accountId -> updatedCashback)
                Future.successful(())
            case None =>
                Future.failed(new IllegalArgumentException(s"Limit with account $accountId does not exist"))
        }
    }

    def getAccountCashback(accountId: Int): Future[Option[AccountCashback]] = {
        Future.successful(accountsCashbacks.get(accountId))
    }

    def calculateCashbackAmount(categoryId: Int, amount: Int): Option[Int] = {
        categoryIdCashbackPercent.get(categoryId) match {
            case Some(cashbackPercent) =>
                val cashback = (amount * cashbackPercent) / 100
                Some(cashback)
            case None => None
        }
    }
}
