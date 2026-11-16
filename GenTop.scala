package learn.core

import _root_.circt.stage.ChiselStage

object GenTop extends App {
  val params = CoreConfig.SmartCore
  
  
  ChiselStage.emitSystemVerilogFile(
    new Top(params),
    args = Array("--target-dir", "generated")
  )
}