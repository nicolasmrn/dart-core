package learn.core.fetch

import chisel3._
import chisel3.util._
import learn.core.CoreParams

class PC(params: CoreParams) extends Module {
  val io = IO(new Bundle {
    val branchTaken   = Input(Bool())
    val branchTarget  = Input(UInt(params.nAddrBit.W))
    val stall         = Input(Bool()) 

    val pcOut         = Output(UInt(params.nAddrBit.W))
    val pcPlusOne     = Output(UInt(params.nAddrBit.W))
  })

  val pcReg = RegInit(params.pcBoot.U(params.nAddrBit.W))

 
  val nextPcPlusOne = pcReg + 1.U

 
  when(io.stall) {
    pcReg := pcReg           
  } .elsewhen(io.branchTaken) {
    pcReg := io.branchTarget 
  } .otherwise {
    pcReg := nextPcPlusOne   
  }

  
  io.pcOut     := pcReg
  
  io.pcPlusOne := nextPcPlusOne(params.nAddrBit - 1, 0) 
}