/**
 * Turn a number into a roman numeral
 * All credits to https://blog.stevenlevithan.com/archives/javascript-roman-numeral-converter
 * @param num
 */
export const romanise = (num: number): string => {
    const lookup: Record<string, number> = {
        M: 1000, CM: 900, D: 500, CD: 400,
        C: 100, XC: 90, L: 50, XL: 40,
        X: 10, IX: 9, V: 5, IV: 4, I: 1
    };
    let roman = '';
    for (const key in lookup) {
        while (num >= lookup[key]) {
            roman += key;
            num -= lookup[key];
        }
    }
    return roman;
};
