#lang racket/base
(require pollen/decode pollen/misc/tutorial txexpr)
(require racket/date txexpr pollen/setup)
(provide (all-defined-out))
 
(module setup racket/base
  (provide (all-defined-out))
  (define poly-targets '(html txt ltx pdf)))
 
(define (get-date)
  (date->string (current-date)))
 
(define (heading . elements)
  (case (current-poly-target)
    [(ltx pdf) (apply string-append `("{\\huge " ,@elements "}"))]
    [(txt) (map string-upcase elements)]
    [else (txexpr 'h2 empty elements)]))
 
(define (emph . elements)
  (case (current-poly-target)
    [(ltx pdf) (apply string-append `("{\\bf " ,@elements "}"))]
    [(txt) `("**" ,@elements "**")]
    [else (txexpr 'strong empty elements)]))

(define (root . elements)
   (txexpr 'root empty (decode-elements elements
     #:txexpr-elements-proc decode-paragraphs
     #:string-proc (compose1 smart-quotes smart-dashes))))
