package learn.core.execute

import chisel3._
import chisel3.util._
import learn.core.Constants._
import learn.core.CoreParams

class ALU(params: CoreParams) extends Module {
  val io = IO(new Bundle {
    val alu_op = Input(UInt(4.W))
    val src1   = Input(UInt(params.nDataBit.W))
    val src2   = Input(UInt(params.nDataBit.W))
    val res    = Output(UInt(params.nDataBit.W))
  })


  val is_sub = (io.alu_op === ALU_SUB) || (io.alu_op === ALU_SLT) || 
               (io.alu_op === ALU_SLTU) || (io.alu_op === ALU_MIN) || (io.alu_op === ALU_MAX)

  val src2_eff = Mux(is_sub, ~io.src2, io.src2)
  val adder    = io.src1 + src2_eff + is_sub.asUInt


  val sign1 = io.src1(params.nDataBit - 1)
  val sign2 = io.src2(params.nDataBit - 1)
  val diff_sign = adder(params.nDataBit - 1)


  val slt  = Mux(sign1 === sign2, diff_sign, sign1)
  

  val sltu = Mux(sign1 === sign2, diff_sign, sign2)


  val shift_amount = io.src2(4, 0)
  val is_sll = (io.alu_op === ALU_SLL)
  val is_sra = (io.alu_op === ALU_SRA)

  
  val shin = Mux(is_sll, Reverse(io.src1), io.src1)
  

  val sign_bit = is_sra & shin(params.nDataBit - 1)
  val shin_ext = Cat(sign_bit, shin).asSInt 


  val shout_ext = (shin_ext >> shift_amount).asUInt
  val shout = shout_ext(params.nDataBit - 1, 0)


  val shift_res = Mux(is_sll, Reverse(shout), shout)


  val add8_res = Cat(
    io.src1(31, 24) + io.src2(31, 24),
    io.src1(23, 16) + io.src2(23, 16),
    io.src1(15,  8) + io.src2(15,  8),
    io.src1( 7,  0) + io.src2( 7,  0)
  )




  io.res := 0.U 

  switch(io.alu_op) {
    is (ALU_ADD)  { io.res := adder }
    is (ALU_SUB)  { io.res := adder }
    is (ALU_AND)  { io.res := io.src1 & io.src2 }
    is (ALU_OR)   { io.res := io.src1 | io.src2 }
    is (ALU_XOR)  { io.res := io.src1 ^ io.src2 }
    is (ALU_SLL)  { io.res := shift_res }
    is (ALU_SRL)  { io.res := shift_res }
    is (ALU_SRA)  { io.res := shift_res }
    is (ALU_SLT)  { io.res := slt.asUInt }
    is (ALU_SLTU) { io.res := sltu.asUInt }
    is (ALU_MIN)  { io.res := Mux(slt, io.src1, io.src2) }
    is (ALU_MAX)  { io.res := Mux(slt, io.src2, io.src1) }
    is (ALU_ADD8) { io.res := add8_res }
  }
}




