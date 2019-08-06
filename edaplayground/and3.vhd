
entity and3 is
	port(in0, in1, in2: in bit;
    	out0: out bit);
end and3;

architecture behav of and3 is
	begin
    	out0 <= in0 and in1 and in2;
end behav;