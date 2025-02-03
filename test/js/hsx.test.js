// test/js/hsx.test.js
import { Button, ButtonViaReactElem, FragmentedButton, SeqButton } from '../../target/test/hsx-test';
import { render, screen, fireEvent } from '@testing-library/react';
import React from "react";

test('Button calls onClick when clicked', () => {
    const handleClick = jest.fn();

    render(<Button onClick={handleClick}>Click Me</Button>);

    // Simulate a click event
    fireEvent.click(screen.getByText(/Click Me/i));
    // Assert the click handler was called
    expect(handleClick).toHaveBeenCalledTimes(1);
});

test('ButtonViaReactElem calls onClick when clicked', () => {
    const handleClick = jest.fn();

    render(<ButtonViaReactElem onClick={handleClick}>Click Me</ButtonViaReactElem>);

    // Simulate a click event
    fireEvent.click(screen.getByText(/Click Me/i));
    // Assert the click handler was called
    expect(handleClick).toHaveBeenCalledTimes(1);
});

test('FragmentedButton calls onClick when clicked', () => {
    const handleClick = jest.fn();

    render(<FragmentedButton onClick={handleClick} buttonOneValue="ButtonOne" buttonTwoValue="ButtonTwo" />)

    // Simulate a click event
    fireEvent.click(screen.getByText(/ButtonOne/i));
    fireEvent.click(screen.getByText(/ButtonTwo/i));

    // Assert the click handler was called
    expect(handleClick).toHaveBeenCalledTimes(2);
});

test('SeqButton calls onClick when clicked', () => {
    const handleClick = jest.fn();

    render(<SeqButton onClick={handleClick} buttonOneValue="ButtonOne" buttonTwoValue="ButtonTwo" />)

    // Simulate a click event
    fireEvent.click(screen.getByText(/ButtonOne/i));
    fireEvent.click(screen.getByText(/ButtonTwo/i));

    // Assert the click handler was called
    expect(handleClick).toHaveBeenCalledTimes(2);
});