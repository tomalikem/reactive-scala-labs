package EShop.lab2

case class Cart(items: Seq[Any]) {
  def contains(item: Any): Boolean = items.contains(item)
  def addItem(item: Any): Cart     = new Cart(items :+ item)
  def removeItem(item: Any): Cart  = new Cart(items.diff(Seq(item)))
  def size: Int                    = items.size
}

object Cart {
  def empty: Cart = new Cart(Seq.empty)
}
