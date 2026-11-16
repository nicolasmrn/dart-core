package learn.core.fetch

import chisel3._
import chisel3.util._
import learn.core.CoreParams

class FetchUnit(params: CoreParams) extends Module {
  val io = IO(new Bundle {

    val branchTaken   = Input(Bool())
    val branchTarget  = Input(UInt(params.nAddrBit.W))
    val stall         = Input(Bool())
    

    val i_addr        = Output(UInt(params.nAddrBit.W)) 
    val i_rdata       = Input(UInt(params.nInstrBit.W)) 
    
    

    val instr_out     = Output(UInt(params.nInstrBit.W))
    val pc_plus_one   = Output(UInt(params.nAddrBit.W))
    val pc_current    = Output(UInt(params.nAddrBit.W))
    
  })

  val pcLogic = Module(new PC(params))

  
  pcLogic.io.branchTaken  := io.branchTaken
  pcLogic.io.branchTarget := io.branchTarget

  pcLogic.io.stall        := io.stall


 
  io.i_addr := pcLogic.io.pcOut

  
  io.instr_out   := io.i_rdata
  
 
  io.pc_plus_one := RegNext(pcLogic.io.pcPlusOne)
  io.pc_current  := RegNext(pcLogic.io.pcOut)
}