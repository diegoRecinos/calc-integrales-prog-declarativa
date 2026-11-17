/*
  SISTEMA SOLUCIONADOR DE INTEGRALES - Programacion Declarativa 02/2026
  Motor simbolico, evaluacion y calculo numerico: exclusivamente SWI-Prolog.
  Java actua solo como interfaz grafica y cliente del proceso.

  Ejecutar en consola:
    swipl -q -s src/prolog/integrales.pl
    ?- resolver("3*x^2+2*x+1",0,2,Primitiva,Definida,Area).

  Ejecutar desde Java:
    swipl -q -s src/prolog/integrales.pl -g puente -t halt
    (enviar tres lineas por stdin: funcion, limite a y limite b)
*/

:- use_module(library(readutil)).

% ===== BASE DE CONOCIMIENTO: REGLAS SIMBOLICAS ============================
% La respuesta representa la primitiva sin la constante de integracion C.
% Cada regla de integracion tiene una justificacion matematica reconocible.

% Constantes, variable y potencias de funciones lineales.
integrar(K, K*x) :- number(K), !.
integrar(x, 0.5*x^2) :- !.
integrar(U^N, C*U^Nuevo) :-
    integer(N), N =\= -1, lineal(U, Pendiente, _), Pendiente =\= 0,
    !, Nuevo is N+1, C is 1/(Pendiente*Nuevo).
integrar(U^(-1), C*ln(abs(U))) :-
    lineal(U, Pendiente, _), Pendiente =\= 0,
    !, C is 1/Pendiente.
integrar(1/U, C*ln(abs(U))) :-
    lineal(U, Pendiente, _), Pendiente =\= 0,
    !, C is 1/Pendiente.

% Linealidad de la integral.
integrar(U+V, FU+FV) :- !, integrar(U,FU), integrar(V,FV).
integrar(U-V, FU-FV) :- !, integrar(U,FU), integrar(V,FV).
integrar(-U, -FU) :- !, integrar(U,FU).
integrar(K*U, K*FU) :- number(K), !, integrar(U,FU).
integrar(U*K, K*FU) :- number(K), !, integrar(U,FU).
integrar(U/K, FU/K) :- number(K), K =\= 0, !, integrar(U,FU).

% Potencias exponenciales: e^u y a^u. Para u lineal: sustitucion simple.
integrar(exp(U), C*exp(U)) :-
    lineal(U,Pendiente,_), Pendiente =\= 0,
    !, C is 1/Pendiente.
integrar(A^U, C*(A^U)) :-
    number(A), A > 0, A =\= 1,
    lineal(U,Pendiente,_), Pendiente =\= 0,
    !, C is 1/(Pendiente*log(A)).
integrar(1^_, x) :- !.

% Trigonometricas directas (incluidas sus variantes con coeficiente y u lineal).
integrar(sin(U), C*cos(U)) :-
    lineal(U,Pendiente,_), Pendiente =\= 0,
    !, C is -1/Pendiente.
integrar(cos(U), C*sin(U)) :-
    lineal(U,Pendiente,_), Pendiente =\= 0,
    !, C is 1/Pendiente.
integrar(tan(U), C*ln(abs(cos(U)))) :-
    lineal(U,Pendiente,_), Pendiente =\= 0,
    !, C is -1/Pendiente.

% Hiperbolicas directas.
integrar(sh(U),F) :- !, integrar(sinh(U),F).
integrar(sinh(U), C*cosh(U)) :-
    lineal(U,Pendiente,_), Pendiente =\= 0,
    !, C is 1/Pendiente.
integrar(cosh(U), C*sinh(U)) :-
    lineal(U,Pendiente,_), Pendiente =\= 0,
    !, C is 1/Pendiente.
integrar(tanh(U), C*ln(cosh(U))) :-
    lineal(U,Pendiente,_), Pendiente =\= 0,
    !, C is 1/Pendiente.

% Hiperbolicas inversas con argumento x (reglas de integracion por partes).
integrar(arcsinh(x), x*arcsinh(x)-sqrt(x^2+1)) :- !.
integrar(arccosh(x), x*arccosh(x)-sqrt(x-1)*sqrt(x+1)) :- !.
integrar(arctanh(x), x*arctanh(x)+0.5*ln(1-x^2)) :- !.
integrar(asinh(x), F) :- !, integrar(arcsinh(x),F).
integrar(acosh(x), F) :- !, integrar(arccosh(x),F).
integrar(atanh(x), F) :- !, integrar(arctanh(x),F).

% Extras opcionales: trigonometricas inversas.
% En el documento "asin(x)" puede representar a*sin(x), no arcsin(x).
% Escribir 3*sin(x) para el caso de coeficiente numerico.
integrar(asin(x), x*asin(x)+sqrt(1-x^2)) :- !.
integrar(acos(x), x*acos(x)-sqrt(1-x^2)) :- !.
integrar(atan(x), x*atan(x)-0.5*ln(1+x^2)) :- !.

% ===== RECONOCIMIENTO DE FUNCIONES AFINES: U = M*X + B ===================
lineal(x, 1, 0) :- !.
lineal(K, 0, K) :- number(K), !.
lineal(-U, M, B) :- !, lineal(U,M0,B0), M is -M0, B is -B0.
lineal(U+V, M, B) :- !,
    lineal(U,M1,B1), lineal(V,M2,B2), M is M1+M2, B is B1+B2.
lineal(U-V, M, B) :- !,
    lineal(U,M1,B1), lineal(V,M2,B2), M is M1-M2, B is B1-B2.
lineal(K*U, M, B) :- number(K), !,
    lineal(U,M0,B0), M is K*M0, B is K*B0.
lineal(U*K, M, B) :- number(K), !,
    lineal(U,M0,B0), M is K*M0, B is K*B0.
lineal(U/K, M, B) :- number(K), K =\= 0, !,
    lineal(U,M0,B0), M is M0/K, B is B0/K.

% ===== LECTURA SEGURA DE EXPRESIONES =====================================
% Solo se admiten terminos matematicos incluidos en la lista blanca.
% No se llama a eval/1, call/1 ni a objetivos construidos por el usuario.
parsear(Entrada, E) :-
    ( string(Entrada) -> atom_string(Atom, Entrada)
    ; atom(Entrada) -> Atom = Entrada
    ; throw(error(type_error(texto,Entrada),_)) ),
    read_term_from_atom(Atom, E, [variable_names(Variables),syntax_errors(error)]),
    ( Variables == [] -> true
    ; throw(error(domain_error(variable_no_permitida,Atom),_)) ),
    ( expresion_permitida(E) -> true
    ; throw(error(domain_error(expresion_no_permitida,Atom),_)) ).

expresion_permitida(E) :- number(E), !.
expresion_permitida(x) :- !.
expresion_permitida(-E) :- !, expresion_permitida(E).
expresion_permitida(E) :-
    compound(E), E =.. [Operador,A,B],
    memberchk(Operador,['+','-','*','/','^']), !,
    expresion_permitida(A), expresion_permitida(B).
expresion_permitida(E) :-
    compound(E), E =.. [F,A],
    memberchk(F,[sin,cos,tan,sh,sinh,cosh,tanh,exp,asin,acos,atan,
                 arcsinh,arccosh,arctanh,asinh,acosh,atanh]), !,
    expresion_permitida(A).

% ===== RESTRICCIONES DE DOMINIO ===========================================
% Impide cruzar discontinuidades conocidas o pedir resultados fuera del
% dominio real de estas familias. No pretende ser un CAS universal.
dominio(E,_,_) :- number(E), !.
dominio(x,_,_) :- !.
dominio(-E,A,B) :- !, dominio(E,A,B).
dominio(E,A,B) :-
    E =.. [Op,U,V], memberchk(Op,['+','-','*','/','^']), !,
    dominio(U,A,B), dominio(V,A,B),
    verificar_operacion(Op,U,V,A,B).
dominio(E,A,B) :- E =.. [tan,U], !,
    dominio(U,A,B),
    ( lineal(U,M,N), M =\= 0 ->
        Inf is min(M*A+N,M*B+N), Sup is max(M*A+N,M*B+N),
        K1 is ceiling((Inf-pi/2)/pi - 1.0e-12),
        K2 is floor((Sup-pi/2)/pi + 1.0e-12),
        ( K1 =< K2 -> throw(error(domain_error(discontinuidad_tangente,tan(U)),_)) ; true )
    ; true ).
dominio(E,A,B) :- E =.. [F,U],
    memberchk(F,[arccosh,acosh]), !, dominio(U,A,B),
    ( U == x, A >= 1 -> true
    ; throw(error(domain_error(arccosh_requiere_x_mayor_igual_a_uno,E),_)) ).
dominio(E,A,B) :- E =.. [F,U],
    memberchk(F,[arctanh,atanh]), !, dominio(U,A,B),
    ( U == x, A > -1, B < 1 -> true
    ; throw(error(domain_error(arctanh_requiere_menos_uno_menor_x_menor_uno,E),_)) ).
dominio(E,A,B) :- E =.. [F,U],
    memberchk(F,[asin,acos]), !, dominio(U,A,B),
    ( U == x, A >= -1, B =< 1 -> true
    ; throw(error(domain_error(arcos_requieren_x_entre_menos_uno_y_uno,E),_)) ).
dominio(E,A,B) :- E =.. [_,U], !, dominio(U,A,B).

verificar_operacion('/',_,Den,A,B) :- !,
    ( number(Den), Den =\= 0 -> true
    ; lineal(Den,M,N), M =\= 0,
      (cero_en_intervalo(M,N,A,B) ->
          throw(error(domain_error(division_entre_cero,Den),_)) ; true)
    ; throw(error(domain_error(division_no_permitida,Den),_)) ).
verificar_operacion('^',Base,Exponente,A,B) :- !,
    ( number(Base) ->
        ( Base > 0 -> true
        ; integer(Exponente), Exponente >= 0 -> true
        ; throw(error(domain_error(base_exponencial_positiva,Base),_)) )
    ; integer(Exponente), Exponente < 0, lineal(Base,M,N), M =\= 0 ->
        ( cero_en_intervalo(M,N,A,B) ->
            throw(error(domain_error(potencia_con_polo,Base),_)) ; true )
    ; true ).
verificar_operacion(_,_,_,_,_).

cero_en_intervalo(M,N,A,B) :- Raiz is -N/M, Raiz >= A-1.0e-12, Raiz =< B+1.0e-12.

% ===== EVALUADOR NUMERICO, TAMBIEN EN PROLOG =============================
evaluar(E,_,Y) :- number(E), !, Y is float(E).
evaluar(x,X,X) :- !.
evaluar(-U,X,Y) :- !, evaluar(U,X,Y0), Y is -Y0.
evaluar(U+V,X,Y) :- !, evaluar(U,X,A), evaluar(V,X,B), Y is A+B.
evaluar(U-V,X,Y) :- !, evaluar(U,X,A), evaluar(V,X,B), Y is A-B.
evaluar(U*V,X,Y) :- !, evaluar(U,X,A), evaluar(V,X,B), Y is A*B.
evaluar(U/V,X,Y) :- !,
    evaluar(U,X,A), evaluar(V,X,B),
    ( B =:= 0 -> throw(error(evaluation_error(zero_divisor),_)) ; Y is A/B ).
evaluar(U^V,X,Y) :- !,
    evaluar(U,X,A), evaluar(V,X,B), Y is A**B.
evaluar(sin(U),X,Y) :- !, evaluar(U,X,A), Y is sin(A).
evaluar(cos(U),X,Y) :- !, evaluar(U,X,A), Y is cos(A).
evaluar(tan(U),X,Y) :- !, evaluar(U,X,A),
    ( abs(cos(A)) < 1.0e-11 -> throw(error(domain_error(tangente_indefinida,A),_))
    ; Y is tan(A) ).
evaluar(exp(U),X,Y) :- !, evaluar(U,X,A), Y is exp(A).
evaluar(sinh(U),X,Y) :- !, evaluar(U,X,A), Y is sinh(A).
evaluar(sh(U),X,Y) :- !, evaluar(sinh(U),X,Y).
evaluar(cosh(U),X,Y) :- !, evaluar(U,X,A), Y is cosh(A).
evaluar(tanh(U),X,Y) :- !, evaluar(U,X,A), Y is tanh(A).
evaluar(ln(U),X,Y) :- !, evaluar(U,X,A),
    ( A > 0 -> Y is log(A)
    ; throw(error(domain_error(logaritmo_no_positivo,A),_)) ).
evaluar(abs(U),X,Y) :- !, evaluar(U,X,A), Y is abs(A).
evaluar(sqrt(U),X,Y) :- !, evaluar(U,X,A),
    ( A >= -1.0e-12 -> Y is sqrt(max(0,A))
    ; throw(error(domain_error(raiz_cuadrada_negativa,A),_)) ).
evaluar(asin(U),X,Y) :- !, evaluar(U,X,A),
    ( A >= -1, A =< 1 -> Y is asin(A)
    ; throw(error(domain_error(arcos_fuera_de_dominio,A),_)) ).
evaluar(acos(U),X,Y) :- !, evaluar(U,X,A),
    ( A >= -1, A =< 1 -> Y is acos(A)
    ; throw(error(domain_error(arcos_fuera_de_dominio,A),_)) ).
evaluar(atan(U),X,Y) :- !, evaluar(U,X,A), Y is atan(A).
evaluar(arcsinh(U),X,Y) :- !, evaluar(U,X,A),
    Y is log(A+sqrt(A*A+1)).
evaluar(asinh(U),X,Y) :- !, evaluar(arcsinh(U),X,Y).
evaluar(arccosh(U),X,Y) :- !, evaluar(U,X,A),
    ( A >= 1 -> Y is log(A+sqrt(A-1)*sqrt(A+1))
    ; throw(error(domain_error(arccosh_fuera_de_dominio,A),_)) ).
evaluar(acosh(U),X,Y) :- !, evaluar(arccosh(U),X,Y).
evaluar(arctanh(U),X,Y) :- !, evaluar(U,X,A),
    ( abs(A) < 1 -> Y is 0.5*log((1+A)/(1-A))
    ; throw(error(domain_error(arctanh_fuera_de_dominio,A),_)) ).
evaluar(atanh(U),X,Y) :- !, evaluar(arctanh(U),X,Y).

valor_seguro(E,X,Y) :- evaluar(E,X,Y),
    ( number(Y), Y =:= Y, abs(Y) < 1.0e100 -> true
    ; throw(error(domain_error(valor_no_finito,E),_)) ).

% ===== SIMPLIFICADOR ALGEBRAICO BASICO ===================================
simplificar(E,S) :- number(E), !, S = E.
simplificar(x,x) :- !.
simplificar(-E,S) :- !, simplificar(E,T),
    ( number(T) -> S is -T ; T = -R -> S = R ; S = -T ).
simplificar(E+F,S) :- !, simplificar(E,A), simplificar(F,B),
    ( A == 0 -> S = B ; B == 0 -> S = A
    ; number(A), number(B) -> S is A+B ; S = A+B ).
simplificar(E-F,S) :- !, simplificar(E,A), simplificar(F,B),
    ( B == 0 -> S = A ; A == 0 -> simplificar(-B,S)
    ; number(A), number(B) -> S is A-B ; S = A-B ).
simplificar(E*F,S) :- !, simplificar(E,A), simplificar(F,B),
    ( (A == 0 ; B == 0) -> S = 0
    ; A == 1 -> S = B ; B == 1 -> S = A
    ; A == -1 -> simplificar(-B,S) ; B == -1 -> simplificar(-A,S)
    ; number(A), number(B) -> S is A*B ; S = A*B ).
simplificar(E/F,S) :- !, simplificar(E,A), simplificar(F,B),
    ( A == 0 -> S = 0 ; B == 1 -> S = A
    ; number(A), number(B), B =\= 0 -> S is A/B ; S = A/B ).
simplificar(E^F,S) :- !, simplificar(E,A), simplificar(F,B),
    ( B == 0 -> S = 1 ; B == 1 -> S = A
    ; number(A), number(B), A > 0 -> S is A**B ; S = A^B ).
simplificar(E,S) :- E =.. [F,A], simplificar(A,B), S =.. [F,B].

% ===== AREA GEOMETRICA: SIMPSON COMPUESTO SOBRE |f(x)| ===================
% Integral definida: F(b)-F(a), puede ser negativa.
% Area geometrica: integral desde min(a,b) a max(a,b) de |f(x)|.
area_geometrica(_, A, A, 0.0) :- !.
area_geometrica(E, A, B, Area) :-
    N = 2000, Paso is (B-A)/N,
    valor_seguro(E,A,Y0), valor_seguro(E,B,YN),
    simpson_interno(1,N,E,A,Paso,0.0,Suma),
    Area is (Paso/3)*(abs(Y0)+abs(YN)+Suma).

simpson_interno(I,N,_,_,_,Ac,Ac) :- I >= N, !.
simpson_interno(I,N,E,A,Paso,Ac,Suma) :-
    X is A+Paso*I, valor_seguro(E,X,Y),
    ( 1 is I mod 2 -> Factor = 4 ; Factor = 2 ),
    Ac1 is Ac+Factor*abs(Y), I1 is I+1,
    simpson_interno(I1,N,E,A,Paso,Ac1,Suma).

% ===== API PUBLICA DE PROLOG =============================================
% resolver(+FuncionTexto,+LimiteA,+LimiteB,-Primitiva,-Integral,-Area)
resolver(Entrada,A,B,Primitiva,Integral,Area) :-
    must_be(number,A), must_be(number,B),
    ( abs(A) < 1.0e5, abs(B) < 1.0e5 -> true
    ; throw(error(domain_error(limites_fuera_de_rango,A-B),_)) ),
    parsear(Entrada,E),
    ( integrar(E,F0) -> true
    ; throw(error(domain_error(integral_no_soportada,E),_)) ),
    simplificar(F0,F),
    Min is min(A,B), Max is max(A,B),
    dominio(E,Min,Max),
    valor_seguro(F,A,FA), valor_seguro(F,B,FB),
    Integral is FB-FA,
    area_geometrica(E,Min,Max,Area),
    term_string(F,Primitiva,[quoted(false)]).

% Puntos para dibujar la grafica, calculados exclusivamente en Prolog.
puntos_grafica(Entrada,A,B,Puntos) :-
    parsear(Entrada,E), Min is min(A,B), Max is max(A,B),
    findall(S,
        (between(0,160,I),
         X is Min+(Max-Min)*I/160,
         valor_seguro(E,X,Y),
         format(string(S),'~12g',[Y])),
        Valores),
    atomics_to_string(Valores,";",Puntos).

% ===== PUENTE POR STDIN/STDOUT HACIA JAVA ================================
% Solicitud:  3 lineas (funcion / a / b)
% Respuesta: OK<TAB>primitiva<TAB>integral<TAB>area
%            PLOT<TAB>valores_del_grafico_separados_por_punto_y_coma
% Error:     ERROR<TAB>mensaje
puente :-
    catch(puente_impl,Error,
        (mensaje_error(Error,Mensaje),
         limpiar_mensaje(Mensaje,Limpio),
         format('ERROR\t~s~n',[Limpio]))).

puente_impl :-
    read_line_to_string(user_input,Funcion),
    read_line_to_string(user_input,TextoA),
    read_line_to_string(user_input,TextoB),
    ( (Funcion == end_of_file ; TextoA == end_of_file ; TextoB == end_of_file) ->
        throw(error(domain_error(entrada_incompleta,solicitud),_)) ; true ),
    number_string(A,TextoA), number_string(B,TextoB),
    resolver(Funcion,A,B,Primitiva,Integral,Area),
    puntos_grafica(Funcion,A,B,Puntos),
    number_string(Integral,TextoIntegral),
    number_string(Area,TextoArea),
    format('OK\t~s\t~s\t~s~n',[Primitiva,TextoIntegral,TextoArea]),
    format('PLOT\t~s~n',[Puntos]).

mensaje_error(error(domain_error(integral_no_soportada,_),_),
    "Esta integral no esta cubierta por las reglas de la base de conocimiento.") :- !.
mensaje_error(error(domain_error(expresion_no_permitida,_),_),
    "Expresion invalida. Utiliza x minuscula, * para multiplicar y ^ para potencias.") :- !.
mensaje_error(error(domain_error(variable_no_permitida,_),_),
    "Solo se admite la variable x en minuscula.") :- !.
mensaje_error(error(domain_error(entrada_incompleta,_),_),
    "Faltan datos de entrada.") :- !.
mensaje_error(error(syntax_error(_),_),
    "La expresion tiene un error de sintaxis. Ejemplo: 3*x^2+2*x+1.") :- !.
mensaje_error(error(domain_error(Tipo,_),_),Mensaje) :- !,
    format(string(Mensaje),'Dominio matematico no valido: ~w.',[Tipo]).
mensaje_error(error(type_error(_,_),_),
    "Formato invalido. Verifica la funcion y los dos limites numericos.") :- !.
mensaje_error(error(evaluation_error(_),_),
    "La evaluacion numerica encontro una division invalida o un valor fuera de rango.") :- !.
mensaje_error(_,
    "No se pudo resolver. Comprueba la expresion y sus limites.").

limpiar_mensaje(Mensaje,Limpio) :-
    split_string(Mensaje,"\t\n\r","\t\n\r",Partes),
    atomics_to_string(Partes," ",Limpio).
